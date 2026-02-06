package com.veritynow.core.store.lock.postgres;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Param;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Ltree;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;
import com.veritynow.core.store.txn.TransactionContext;
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
 * 
 * 
 * 
 */
public class PgLockingService implements LockingService {

	private final static String PG_ADVISORY_LOCK = "pg_try_advisory_xact_lock";
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final DataSource dataSource;
    private final DSLContext defaultDSL;
    private final int maxDelay = 1000;

	// No background renewal thread.
	// We rely on the DB-visible expires_at field (and the conflict query ignores
	// expired leases).

	public PgLockingService(DSLContext dsl, DataSource dataSource) {
		this.defaultDSL = Objects.requireNonNull(dsl, "dsl required");
		this.dataSource = dataSource;
		LOGGER.info("Postgres Locking Service started.");
	}
	
	
	private DSLContext ensureDSL() {
    	if (!Context.isActive()) {
    		return defaultDSL;
    	}
    	String txnId = Context.transactionIdOrNull();
    	if (txnId == null) {
    		return defaultDSL;
    	}
    	Connection conn = TransactionContext.getConnection(txnId);
    	if (conn == null) {
    		//try lock called first before begin()
    		//lets make a connection for this transaction
    		if (conn == null) {
    			// try to get a valid connection and bind it to transactionId
    			// this will be the connection for this transaction
    			// it is not thread bound
    			try {
    				// Connection
    				conn = dataSource.getConnection();
    				conn.setAutoCommit(false);
    			} catch (SQLException e) {
    				try {
    					if (conn != null)
    						conn.close();
    				} catch (Exception ignore) {
    				}
    				;
    				throw new IllegalStateException("Cannot set the state of the connection ", e);
    			}

    			TransactionContext.putConnection(txnId, conn);
    		}
    	}
    	
   		return DSL.using(conn, SQLDialect.POSTGRES);
    }
	private boolean hasTxnConnection() {
		if (!Context.isActive()) return false;
		String txnId = Context.transactionIdOrNull();
		if (txnId == null || txnId.isBlank()) return false;
		return TransactionContext.getConnection(txnId) != null;
	}

	/**
	 * For CREATE of a non-existent path, there is no inode row yet to hard-lock.
	 * We bridge that gap by taking a transaction-scoped advisory lock on a stable
	 * 64-bit key derived from the requested scope. This lock is held until the
	 * surrounding DB transaction commits/rolls back.
	 *
	 * If no transaction-bound connection exists (non-txn / probe usage), this
	 * method is a no-op and hard-locking falls back to nearest existing ancestors.
	 */
	private void tryAcquireCreateAdvisoryLockNowait(String txnId, String scopeKeyString, DSLContext dsl) {
		if (!hasTxnConnection()) return; // advisory_xact_lock only makes sense inside a real txn
		
		long key = PathKeyCodec.scopeKeyToLockKey(scopeKeyString);

		if (!acquireAdvisoryLock(key, dsl)) {
			throw new IllegalStateException("Lock conflict");
		}
		TransactionContext.putLock(txnId, key);
	}

	
	
	private boolean acquireAdvisoryLock(Long key, DSLContext dsl) {
		Boolean ok = dsl.select(
				DSL.function(PG_ADVISORY_LOCK, Boolean.class, DSL.val(key))
			).fetchOne(0, Boolean.class);
		if  (ok != null) return ok.booleanValue();
		return false;		
	}
	
	private Long resolveExactInodeId(String scopeKeyString, DSLContext dsl) {
		if (scopeKeyString == null || scopeKeyString.isBlank()) return null;
		
		return dsl.select(VN_INODE.ID)
				.from(VN_INODE)
				.where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(scopeKeyString)))
				.fetchOne(VN_INODE.ID);
	}

	
	@Override
	public LockHandle acquire(List<String> paths) {
		Objects.requireNonNull(paths, "paths");
		
		ContextScope ctx = Context.ensureContext("Acquired Lock");

		ContextSnapshot snap = Context.snapshot();
		String txnId = snap.transactionIdOrNull();
		if (txnId == null || txnId.isBlank())
			txnId = snap.correlationId();
		if (txnId == null || txnId.isBlank())
			throw new IllegalStateException("No transactionId/correlationId in Context");

		
		
		List<String> normalized = paths.stream().filter(Objects::nonNull).map(PathUtils::normalizePath).distinct()
				.sorted().toList();

		if (normalized.isEmpty())
			throw new IllegalArgumentException("No scopes provided");
		
		// store-owned codec
		List<String> scopeKeyStrings = normalized.stream().map(PathKeyCodec::toLTree).sorted().toList();

		List<String> minimizedScoped = minimizeScopes(scopeKeyStrings);

		lockScopeRootsNowait(txnId, minimizedScoped);

		return new LockHandle(txnId, ctx);

	}

	@Override
	public void release(LockHandle handle) {
		handle.scope().close();
	}

	@Override
	public List<Long> findActiveAdvisoryLocks(String txnId) {
		return TransactionContext.getActiveAdvisoryLocks(txnId);
	}

	@Override
	public Optional<Long> findActiveAdvisoryLock(String txnId, String path) {
		List<Long> ls = findActiveAdvisoryLocks(txnId);
			Long key = PathKeyCodec.pathToLockKey(path);
			int idx = ls.indexOf(key);
			if (idx > -1) {
				return Optional.of(ls.get(idx));
			}
		return Optional.empty();
	}
	
	@Override
	public LockHandle tryAcquireLock(List<String> paths, int maxAttempts, int delayBetweenAttemptsMs) {
		Objects.requireNonNull(paths, "paths");
		int tries = Math.max(1, maxAttempts);
		int attempt = 0;
		while (true) {
			try {
				attempt++;
				return acquire(paths);
			} catch (IllegalStateException e) {
				tries--;
				if (tries <= 0)
					throw e;
				ProcessUtil.sleep(ProcessUtil.backoffWithJitter(delayBetweenAttemptsMs, attempt, maxDelay));
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
	private void lockScopeRootsNowait(String txnId, List<String> scopeKeyStrings) {
		
		if (scopeKeyStrings == null || scopeKeyStrings.isEmpty())
			return;

		// Resolve each requested scope to the nearest existing inode in the ancestor
		// chain.
		// (e.g. when the exact path doesn't exist yet, we lock the closest existing
		// parent.)
		DSLContext dsl = ensureDSL();
		
		List<Long> rootIds = new ArrayList<>(scopeKeyStrings.size());
		for (String scopeKey : scopeKeyStrings) {
			// If the exact path doesn't exist yet, we cannot hard-lock a leaf inode row.
			// Bridge the create-race by taking a txn-scoped advisory lock on the scope key.
			Long exact = resolveExactInodeId(scopeKey, dsl);
			if (exact == null) {
				tryAcquireCreateAdvisoryLockNowait(txnId, scopeKey, dsl);
			}
			
			
			Long id = (exact != null) ? exact : resolveNearestExistingInodeId(scopeKey, dsl);
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
	 * Resolve the nearest existing inode id for the requested scope.
	 * <p>
	 * Uses {@code ltree} containment to find the deepest ancestor (including the
	 * node itself if it exists).
	 */
	private Long resolveNearestExistingInodeId(String scopeKeyString, DSLContext dsl) {
		if (scopeKeyString == null || scopeKeyString.isBlank()) return null;

		 
		Param<Ltree> target = DSL.val(Ltree.ltree(scopeKeyString), VN_INODE.SCOPE_KEY.getDataType());

		return dsl
		  .select(VN_INODE.ID)
		  .from(VN_INODE)
		  .where(DSL.condition("{0} @> {1}", VN_INODE.SCOPE_KEY, target))
		  .orderBy(DSL.function("nlevel", Integer.class, VN_INODE.SCOPE_KEY).desc())
		  .limit(1)
		  .fetchOne(VN_INODE.ID);
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
