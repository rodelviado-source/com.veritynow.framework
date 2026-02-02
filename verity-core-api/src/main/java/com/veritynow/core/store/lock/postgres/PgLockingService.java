package com.veritynow.core.store.lock.postgres;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.postgres.extensions.types.Ltree;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;
import com.veritynow.core.store.versionstore.PathUtils;
import com.veritynow.core.store.versionstore.repo.PathKeyCodec;

import util.ProcessUtil;

/**
 * Postgres locking kernel: exclusive subtree locking using ltree scope keys.
 *
 * jOOQ version (no JdbcTemplate): - conflict detection uses explicit CROSS JOIN
 * unnest(text[]) and ltree @>/<@ predicates - fence token from
 * VN_FENCE_TOKEN_SEQ - lock group / path locks inserted via DSL - release /
 * renew via DSL This implementation does NOT use any lock auxiliary tables.
 * Instead, it acquires row locks on {@code vn_inode} rows that represent the
 * target paths (and, optionally, their descendants) by using
 * {@code SELECT ... FOR UPDATE NOWAIT}.
 *
 * Key properties: - Exclusivity: overlapping subtree locks contend via row
 * locks. - No lock bookkeeping tables: release is effectively a no-op (locks
 * are released automatically by PostgreSQL when the surrounding transaction
 * ends). - Deterministic acquisition order: lock rows ordered by inode_id to
 * reduce deadlocks.
 *
 * Important: row locks are connection/transaction-scoped. To keep locks held
 * while work is performed, callers must run acquire() and subsequent work in
 * the SAME Spring @Transactional boundary (or otherwise keep the same JDBC
 * connection open).
 * 
 */
public class PgLockingService implements LockingService {

	private static final Logger LOGGER = LogManager.getLogger();

	private final DSLContext dsl;

	/** Lease TTL in milliseconds. When <= 0, leases disabled. */
	@SuppressWarnings("unused")
	private final long ttlMs;

	// No background renewal thread.
	// We rely on the DB-visible expires_at field (and the conflict query ignores
	// expired leases).

	public PgLockingService(DSLContext dsl, long ttlMs, float lockRenewFraction) {
		this.dsl = Objects.requireNonNull(dsl, "dsl required");
		this.ttlMs = ttlMs;

		if (ttlMs > 0) {
			LOGGER.info("Postgres Locking Service started. [ttl({ignored})]", ttlMs);
		} else {
			LOGGER.info("Postgres Locking Service started. [ttl(ignored)]");

		}
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public LockHandle acquire(List<String> paths) {
		Objects.requireNonNull(paths, "paths");
		
		Context.ensureContext("Acquired Lock");

		ContextSnapshot snap = Context.snapshot();
		String ownerId = snap.transactionIdOrNull();
		if (ownerId == null || ownerId.isBlank())
			ownerId = snap.correlationId();
		if (ownerId == null || ownerId.isBlank())
			throw new IllegalStateException("No transactionId/correlationId in Context");

		List<String> normalized = paths.stream().filter(Objects::nonNull).map(PathUtils::normalizePath).distinct()
				.sorted().toList();

		if (normalized.isEmpty())
			throw new IllegalArgumentException("No scopes provided");
		
		// store-owned codec
		List<String> scopeKeyStrings = normalized.stream().map(PathKeyCodec::toLTree).sorted().toList();

		List<String> minimizedScoped = minimizeScopes(scopeKeyStrings);

		lockScopeRootsNowait(minimizedScoped);

		return new LockHandle(ownerId);

	}

	@Override
	public void release(LockHandle handle) {
		if (Context.isActive() && "Acquired Lock".equals( Context.contextNameOrNull()) )
			Context.scope().close();
		// Release is always idempotent and safe to call from finally blocks.
	}

	@Override
	public Optional<LockHandle> findActiveLock(String ownerId) {
		return Optional.empty();
	}

	@Override
	public LockHandle tryAcquireLock(List<String> paths, int maxTries, int intervalBetweenTriesMs, int jitterMs) {
		Objects.requireNonNull(paths, "paths");
		int tries = Math.max(1, maxTries);
		while (true) {
			try {
				return acquire(paths);
			} catch (IllegalStateException e) {
				tries--;
				if (tries <= 0)
					throw e;
				ProcessUtil.sleep(Duration.ofMillis(ProcessUtil.jitter(intervalBetweenTriesMs, jitterMs)));
			}
		}
	}

	/**
	 * Locks only the *root inodes* for the requested scopes (NOT the entire
	 * subtree).
	 *
	 * Enforcement rule is delegated to a DB trigger on vn_node_head: before a HEAD
	 * move, the trigger should attempt to lock ancestor vn_inode rows (including
	 * the root scope) using NOWAIT.
	 */
	private void lockScopeRootsNowait(List<String> scopeKeyStrings) {
		if (scopeKeyStrings == null || scopeKeyStrings.isEmpty())
			return;

		// Resolve each requested scope to the nearest existing inode in the ancestor
		// chain.
		// (e.g. when the exact path doesn't exist yet, we lock the closest existing
		// parent.)
		List<Long> rootIds = new ArrayList<>(scopeKeyStrings.size());
		for (String scopeKey : scopeKeyStrings) {
			Long id = resolveNearestExistingInodeId(scopeKey);
			if (id == null) {
				throw new IllegalStateException("No inode exists for scopeKey='" + scopeKey + "' or any ancestor");
			}
			rootIds.add(id);
		}

		// Deterministic lock order.
		rootIds = rootIds.stream().distinct().sorted().toList();

		try {
			// Lock order is by inode_id for determinism.
			dsl.select(VN_INODE.ID).from(VN_INODE).where(VN_INODE.ID.in(rootIds)).orderBy(VN_INODE.ID.asc()).forUpdate()
					.noWait().fetch();
		} catch (DataAccessException dae) {
			String msg = dae.getMessage();
			if (msg != null && msg.contains("could not obtain lock")) {
				throw new IllegalStateException("Lock conflict", dae);
			}
			throw dae;
		}
	}

	/**
	 * Best-effort: resolve the exact inode by scope_key; if missing, walk up the
	 * ltree label chain and return the nearest ancestor inode id.
	 */
	private Long resolveNearestExistingInodeId(String scopeKeyString) {
		if (scopeKeyString == null || scopeKeyString.isBlank())
			return null;
		// Fast path: exact match.
		Long id = dsl.select(VN_INODE.ID).from(VN_INODE).where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(scopeKeyString)))
				.fetchOne(VN_INODE.ID);
		if (id != null)
			return id;

		// Walk up: "a.b.c" -> "a.b" -> "a".
		String[] labels = scopeKeyString.split("\\.");
		for (int n = labels.length - 1; n >= 1; n--) {
			String anc = String.join(".", java.util.Arrays.copyOf(labels, n));
			id = dsl.select(VN_INODE.ID).from(VN_INODE).where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(anc)))
					.fetchOne(VN_INODE.ID);
			if (id != null)
				return id;
		}
		return null;
	}

	// ---------- jOOQ kernel ops ----------

	// ---------- scope minimization (unchanged) ----------

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
			if (!covered)
				out.add(s);
		}
		return out;
	}

	private static boolean isAncestorOrSame(String a, String b) {
		if (a.equals(b))
			return true;
		if (PathKeyCodec.ROOT_LABEL.equals(a))
			return true;
		return b.startsWith(a + ".");
	}

}
