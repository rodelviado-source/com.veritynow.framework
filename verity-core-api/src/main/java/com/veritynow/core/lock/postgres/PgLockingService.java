package com.veritynow.core.lock.postgres;

import java.sql.Array;
import java.sql.PreparedStatement;
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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;
import com.veritynow.core.store.jpa.PathKeyCodec;
import com.veritynow.core.store.jpa.PathUtils;

/**
 * Postgres locking kernel: exclusive subtree locking using ltree scope keys.
 *
 * Alignment rules:
 * - Locking does not compute segment hashes or ltree codecs.
 * - Store provides precomputed ltree scope keys (per-inode) for requested paths.
 */
public class PgLockingService implements LockingService {

    private final JdbcTemplate jdbc;

    /**
     * Lease TTL in milliseconds. When negative, leases are disabled and locks behave as before (no expiry).
     */
    private final long ttlMs;

    /**
     * Renewal interval in milliseconds (only used when ttlMs > 0).
     */
    private final long renewEveryMs;

    private final ScheduledExecutorService renewScheduler;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> renewTasks = new ConcurrentHashMap<>();
    

    public PgLockingService(JdbcTemplate jdbc, long ttlMs) {
        this.jdbc = Objects.requireNonNull(jdbc, "JDBC required");
        this.ttlMs = ttlMs;

        if (ttlMs > 0) {
            // Renew at ~1/3 TTL; clamp to a reasonable minimum to avoid a tight loop.
            this.renewEveryMs = Math.max(250L, ttlMs / 3L);
            this.renewScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
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

    /**
     * Backwards-compatible constructor: TTL disabled (pre-TTL behavior).
     */
    public PgLockingService(JdbcTemplate jdbc) {
        this(jdbc, -1L);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public LockHandle acquire(List<String> paths) {
        Objects.requireNonNull(paths, "paths");

        ContextSnapshot snap = Context.snapshot();
        String ownerId = snap.transactionIdOrNull();
        
        if (ownerId == null || ownerId.isBlank()) {
            ownerId = snap.correlationId();
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalStateException("No transactionId/correlationId in Context");
        }

        List<String> normalized = paths.stream()
                .filter(Objects::nonNull)
                .map(PathUtils::normalizePath)
                .distinct()
                .sorted()
                .toList();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("No scopes provided");
        }

        List<String> minimalScopes = minimizeScopes(normalized);

        // Resolve ltree scope keys using store-owned codec logic. The inode may not exist yet
        // (e.g., callers lock an intent path before creation), so we must not require resolution
        // through the inode/direntry graph here.
        List<String> scopeKeys = minimalScopes.stream()
                .map(PathKeyCodec::toLTree)
                .toList();

        // 1) Conflict check
        if (existsConflicts(ownerId, scopeKeys)) {
            throw new IllegalStateException("Lock conflict");
        }

        // 2) Allocate fence token
        long fenceToken = nextFenceToken();

        // 3) Create lock group
        UUID lockGroupId = UUID.randomUUID();
        insertLockGroup(lockGroupId, ownerId, fenceToken);

        // 4) Insert lock rows (scope keys already derived by store)
        insertPathLocks(lockGroupId, ownerId, scopeKeys);

        // 5) For transparency/debugging, capture derived scope keys as text
        //List<String> scopeKeys = resolveScopeKeys(minimalScopes);

        LockHandle handle = new LockHandle(ownerId, lockGroupId, fenceToken);
        startLeaseRenewal(handle);
        return handle;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(LockHandle handle) {
        if (handle == null) return;

        stopLeaseRenewal(handle.lockGroupId());

        jdbc.update(
            "UPDATE vn_path_lock " +
            "SET active = false, released_at = now() " +
            "WHERE lock_group_id = ? AND active = true",
            handle.lockGroupId()
        );

        jdbc.update(
            "UPDATE vn_lock_group " +
            "SET active = false, released_at = now() " +
            "WHERE lock_group_id = ? AND active = true",
            handle.lockGroupId()
        );
    }

    private boolean existsConflicts(String ownerId, List<String> scopeKeys) {
        Integer v = jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(
                // Compute candidate ltree[] once, then use for overlap predicates.
                "WITH keys AS (" +
                "  SELECT array_agg(p::ltree) AS ks " +
                "  FROM unnest(?::text[]) AS p" +
                ") " +
                "SELECT CASE WHEN EXISTS (" +
                "  SELECT 1 " +
                "  FROM vn_path_lock pl " +
                "  JOIN vn_lock_group lg ON lg.lock_group_id = pl.lock_group_id " +
                "  CROSS JOIN keys " +
                "  WHERE pl.active = true " +
                "    AND lg.active = true " +
                "    AND (lg.expires_at IS NULL OR lg.expires_at > now()) " +
                "    AND pl.owner_id <> ? " +
                "    AND ( " +
                "      pl.scope_key @> ANY (keys.ks) " +
                "      OR pl.scope_key <@ ANY (keys.ks) " +
                "    )" +
                ") THEN 1 ELSE 0 END"
            );

            Array arr = con.createArrayOf("text", scopeKeys.toArray(String[]::new));
            ps.setArray(1, arr);
            ps.setString(2, ownerId);
            return ps;
        }, rs -> rs.next() ? rs.getInt(1) : 0);

        return v != null && v.intValue() == 1;
    }

    private long nextFenceToken() {
        Long v = jdbc.queryForObject("select nextval('vn_fence_token_seq')", Long.class);
        if (v == null) throw new IllegalStateException("Failed to allocate fence token");
        return v;
    }

    private void insertLockGroup(UUID lockGroupId, String ownerId, long fenceToken) {
        if (ttlMs > 0) {
            jdbc.update(
                "INSERT INTO vn_lock_group(lock_group_id, owner_id, fence_token, active, acquired_at, expires_at) " +
                "VALUES (?, ?, ?, true, now(), now() + (? * interval '1 millisecond'))",
                lockGroupId, ownerId, fenceToken, ttlMs
            );
        } else {
            // TTL disabled: expires_at stays NULL, meaning infinite lease.
            jdbc.update(
                "INSERT INTO vn_lock_group(lock_group_id, owner_id, fence_token, active, acquired_at, expires_at) " +
                "VALUES (?, ?, ?, true, now(), NULL)",
                lockGroupId, ownerId, fenceToken
            );
        }
    }

    private void startLeaseRenewal(LockHandle handle) {
        if (handle == null) return;
        if (ttlMs <= 0) return;
        if (renewScheduler == null) return;

        // Avoid duplicate tasks for the same lock group.
        renewTasks.computeIfAbsent(handle.lockGroupId(), lgid -> {
            Runnable r = () -> {
                try {
                    boolean ok = renewLease(handle);
                    if (!ok) {
                        stopLeaseRenewal(lgid);
                    }
                } catch (Throwable t) {
                    // Renewal failures should not crash the process. If renewal is unstable,
                    // allow the lease to expire and let fencing prevent stale publishes.
                    stopLeaseRenewal(lgid);
                }
            };

            // Initial delay: renewEveryMs; fixed rate keeps leases stable under moderate jitter.
            return renewScheduler.scheduleAtFixedRate(r, renewEveryMs, renewEveryMs, TimeUnit.MILLISECONDS);
        });
    }

    private void stopLeaseRenewal(UUID lockGroupId) {
        if (lockGroupId == null) return;
        ScheduledFuture<?> f = renewTasks.remove(lockGroupId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private boolean renewLease(LockHandle handle) {
        // Extend only if we still own the lock and it hasn't expired.
        int rows = jdbc.update(
            "UPDATE vn_lock_group " +
            "SET expires_at = now() + (? * interval '1 millisecond') " +
            "WHERE lock_group_id = ? " +
            "  AND active = true " +
            "  AND owner_id = ? " +
            "  AND fence_token = ? " +
            "  AND (expires_at IS NULL OR expires_at > now())",
            ttlMs,
            handle.lockGroupId(),
            handle.ownerId(),
            handle.fenceToken()
        );
        return rows == 1;
    }

    private void insertPathLocks(UUID lockGroupId, String ownerId, List<String> scopeKeys) {
        // Insert one lock row per scope. scope_key is already derived by the store.
        jdbc.batchUpdate(
            "INSERT INTO vn_path_lock(lock_group_id, owner_id, scope_key, active, acquired_at) " +
            "VALUES (?, ?, ?::ltree, true, now())",
            scopeKeys,
            200,
            (ps, scope) -> {
                ps.setObject(1, lockGroupId);
                ps.setString(2, ownerId);
                ps.setString(3, scope);
            }
        );
    }

    private static List<String> minimizeScopes(List<String> sortedScopes) {
        List<String> out = new ArrayList<>();
        for (String s : sortedScopes) {
            boolean covered = false;
            for (String kept : out) {
                if (isAncestorOrSame(kept, s)) {
                    covered = true;
                    break;
                }
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
