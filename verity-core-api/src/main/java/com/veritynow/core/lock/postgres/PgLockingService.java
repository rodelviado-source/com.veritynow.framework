package com.veritynow.core.lock.postgres;

import static com.veritynow.core.store.persistence.jooq.Sequences.VN_FENCE_TOKEN_SEQ;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_LOCK_GROUP;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_PATH_LOCK;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Table;
import org.jooq.postgres.extensions.types.Ltree;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;
import com.veritynow.core.store.db.PathUtils;
import com.veritynow.core.store.db.repo.PathKeyCodec;

/**
 * Postgres locking kernel: exclusive subtree locking using ltree scope keys.
 *
 * jOOQ version (no JdbcTemplate):
 * - conflict detection uses explicit CROSS JOIN unnest(text[]) and ltree @>/<@ predicates
 * - fence token from VN_FENCE_TOKEN_SEQ
 * - lock group / path locks inserted via DSL
 * - release / renew via DSL
 */
public class PgLockingService implements LockingService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DSLContext dsl;

    /** Lease TTL in milliseconds. When <= 0, leases disabled. */
    private final long ttlMs;

    /** Renewal interval in milliseconds (only used when ttlMs > 0). */
    private final long renewEveryMs;

    private final ScheduledExecutorService renewScheduler;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> renewTasks = new ConcurrentHashMap<>();

    public PgLockingService(DSLContext dsl, long ttlMs, float lockRenewFraction) {
        this.dsl = Objects.requireNonNull(dsl, "dsl required");
        this.ttlMs = ttlMs;

        LOGGER.info("Postgres Locking Service started ttl = {}", ttlMs);

        if (ttlMs > 0) {
            this.renewEveryMs = Math.max(250L, Float.valueOf(ttlMs * lockRenewFraction).longValue());
            this.renewScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "verity-lock-lease-renewer");
                    t.setDaemon(true);
                    return t;
                }
            });
        } else {
            this.renewEveryMs = -1L;
            this.renewScheduler = null;
        }
    }

    /** Backwards-compatible: TTL disabled. */
    public PgLockingService(DSLContext dsl) {
        this(dsl, -1L, 1);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public LockHandle acquire(List<String> paths) {
        Objects.requireNonNull(paths, "paths");

        ContextSnapshot snap = Context.snapshot();
        String ownerId = snap.transactionIdOrNull();
        if (ownerId == null || ownerId.isBlank()) ownerId = snap.correlationId();
        if (ownerId == null || ownerId.isBlank()) throw new IllegalStateException("No transactionId/correlationId in Context");

        List<String> normalized = paths.stream()
            .filter(Objects::nonNull)
            .map(PathUtils::normalizePath)
            .distinct()
            .sorted()
            .toList();

        if (normalized.isEmpty()) throw new IllegalArgumentException("No scopes provided");

        List<String> minimalScopes = minimizeScopes(normalized);

        // store-owned codec
        List<String> scopeKeyStrings = minimalScopes.stream()
            .map(PathKeyCodec::toLTree)
            .toList();

        // 1) conflict check
        if (existsConflicts(ownerId, scopeKeyStrings)) {
            throw new IllegalStateException("Lock conflict");
        }

        // 2) fence token
        long fenceToken = nextFenceToken();

        // 3) lock group
        UUID lockGroupId = UUID.randomUUID();
        insertLockGroup(lockGroupId, ownerId, fenceToken);

        // 4) lock rows
        insertPathLocks(lockGroupId, ownerId, scopeKeyStrings);

        LockHandle handle = new LockHandle(ownerId, lockGroupId, fenceToken);
        startLeaseRenewal(handle);
        return handle;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(LockHandle handle) {
        if (handle == null) return;

        stopLeaseRenewal(handle.lockGroupId());

        dsl.update(VN_PATH_LOCK)
           .set(VN_PATH_LOCK.ACTIVE, false)
           .set(VN_PATH_LOCK.RELEASED_AT, currentOffsetDateTime())
           .where(VN_PATH_LOCK.LOCK_GROUP_ID.eq(handle.lockGroupId())
               .and(VN_PATH_LOCK.ACTIVE.eq(true)))
           .execute();

        dsl.update(VN_LOCK_GROUP)
           .set(VN_LOCK_GROUP.ACTIVE, false)
           .set(VN_LOCK_GROUP.RELEASED_AT, currentOffsetDateTime())
           .where(VN_LOCK_GROUP.LOCK_GROUP_ID.eq(handle.lockGroupId())
               .and(VN_LOCK_GROUP.ACTIVE.eq(true)))
           .execute();
    }

    // ---------- jOOQ kernel ops ----------

    private boolean existsConflicts(String ownerId, List<String> scopeKeyStrings) {
        // Derived table: unnest(text[]) -> p(p)
        // We cast each p to ltree on the fly, preserving your original SQL semantics.
        Table<?> p = table("unnest({0}::text[]) as p(p)", val(scopeKeyStrings.toArray(String[]::new)));
        Field<String> pText = field(name("p", "p"), String.class);
        Field<Ltree> k = field("{0}::ltree", Ltree.class, pText);

        // pl.scope_key @> k OR pl.scope_key <@ k
        // Use PlainSQL conditions to preserve operator semantics with your LTree binding.
        var overlap =
            condition("{0} @> {1}", VN_PATH_LOCK.SCOPE_KEY, k)
               .or(condition("{0} <@ {1}", VN_PATH_LOCK.SCOPE_KEY, k));

        return dsl.fetchExists(
            dsl.selectOne()
               .from(VN_PATH_LOCK)
               .join(VN_LOCK_GROUP).on(VN_LOCK_GROUP.LOCK_GROUP_ID.eq(VN_PATH_LOCK.LOCK_GROUP_ID))
               .crossJoin(p)
               .where(VN_PATH_LOCK.ACTIVE.eq(true))
               .and(VN_LOCK_GROUP.ACTIVE.eq(true))
               .and(condition("( {0} is null OR {0} > now() )", VN_LOCK_GROUP.EXPIRES_AT))
               .and(VN_PATH_LOCK.OWNER_ID.ne(ownerId))
               .and(overlap)
        );
    }

    private long nextFenceToken() {
    	Long v = dsl.select(VN_FENCE_TOKEN_SEQ.nextval()).fetchOne(0, Long.class);
        if (v == null) throw new IllegalStateException("Failed to allocate fence token");
        return v.longValue();
    }

    private void insertLockGroup(UUID lockGroupId, String ownerId, long fenceToken) {
        if (ttlMs > 0) {
            // expires_at = now() + ttlMs * interval '1 millisecond'
            Field<OffsetDateTime> expiresAt =
                field("now() + ({0} * interval '1 millisecond')", OffsetDateTime.class, val(ttlMs));

            dsl.insertInto(VN_LOCK_GROUP)
               .set(VN_LOCK_GROUP.LOCK_GROUP_ID, lockGroupId)
               .set(VN_LOCK_GROUP.OWNER_ID, ownerId)
               .set(VN_LOCK_GROUP.FENCE_TOKEN, fenceToken)
               .set(VN_LOCK_GROUP.ACTIVE, true)
               .set(VN_LOCK_GROUP.ACQUIRED_AT, currentOffsetDateTime())
               .set(VN_LOCK_GROUP.EXPIRES_AT, expiresAt)
               .execute();
        } else {
            dsl.insertInto(VN_LOCK_GROUP)
               .set(VN_LOCK_GROUP.LOCK_GROUP_ID, lockGroupId)
               .set(VN_LOCK_GROUP.OWNER_ID, ownerId)
               .set(VN_LOCK_GROUP.FENCE_TOKEN, fenceToken)
               .set(VN_LOCK_GROUP.ACTIVE, true)
               .set(VN_LOCK_GROUP.ACQUIRED_AT, currentOffsetDateTime())
               .setNull(VN_LOCK_GROUP.EXPIRES_AT)
               .execute();
        }
    }

    private void insertPathLocks(UUID lockGroupId, String ownerId, List<String> scopeKeyStrings) {
        // No synthesis: scope keys are store-derived; acquired_at has DB default, but we can set explicitly as before.
        List<Query> inserts = new ArrayList<>(scopeKeyStrings.size());
        for (String s : scopeKeyStrings) {
            inserts.add(
                dsl.insertInto(VN_PATH_LOCK)
                   .set(VN_PATH_LOCK.LOCK_GROUP_ID, lockGroupId)
                   .set(VN_PATH_LOCK.OWNER_ID, ownerId)
                   .set(VN_PATH_LOCK.SCOPE_KEY, Ltree.ltree(s))
                   .set(VN_PATH_LOCK.ACTIVE, true)
                   .set(VN_PATH_LOCK.ACQUIRED_AT, currentOffsetDateTime())
            );
        }
        dsl.batch(inserts).execute();
    }

    private boolean renewLease(LockHandle handle) {
        if (ttlMs <= 0) return true;

        Field<OffsetDateTime> newExpiry =
            field("now() + ({0} * interval '1 millisecond')", OffsetDateTime.class, val(ttlMs));

        int rows = dsl.update(VN_LOCK_GROUP)
            .set(VN_LOCK_GROUP.EXPIRES_AT, newExpiry)
            .where(VN_LOCK_GROUP.LOCK_GROUP_ID.eq(handle.lockGroupId()))
            .and(VN_LOCK_GROUP.ACTIVE.eq(true))
            .and(VN_LOCK_GROUP.OWNER_ID.eq(handle.ownerId()))
            .and(VN_LOCK_GROUP.FENCE_TOKEN.eq(handle.fenceToken()))
            .and(condition("( {0} is null OR {0} > now() )", VN_LOCK_GROUP.EXPIRES_AT))
            .execute();

        return rows == 1;
    }

    // ---------- lease scheduler (unchanged semantics) ----------

    private void startLeaseRenewal(LockHandle handle) {
        if (handle == null) return;
        if (ttlMs <= 0) return;
        if (renewScheduler == null) return;

        renewTasks.computeIfAbsent(handle.lockGroupId(), lgid -> {
            Runnable r = () -> {
                try {
                    boolean ok = renewLease(handle);
                    if (!ok) stopLeaseRenewal(lgid);
                } catch (Throwable t) {
                    stopLeaseRenewal(lgid);
                }
            };
            return renewScheduler.scheduleAtFixedRate(r, renewEveryMs, renewEveryMs, TimeUnit.MILLISECONDS);
        });
    }

    private void stopLeaseRenewal(UUID lockGroupId) {
        if (lockGroupId == null) return;
        ScheduledFuture<?> f = renewTasks.remove(lockGroupId);
        if (f != null) f.cancel(false);
    }

    // ---------- scope minimization (unchanged) ----------

    private static List<String> minimizeScopes(List<String> sortedScopes) {
        List<String> out = new ArrayList<>();
        for (String s : sortedScopes) {
            boolean covered = false;
            for (String kept : out) {
                if (isAncestorOrSame(kept, s)) { covered = true; break; }
            }
            if (!covered) out.add(s);
        }
        return out;
    }

    private static boolean isAncestorOrSame(String a, String b) {
        if (a.equals(b)) return true;
        if ("/".equals(a)) return true;
        return b.startsWith(a + "/");
    }
}
