package com.veritynow.core.store.jpa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

/**
 * Postgres-only auxiliary DDL for VerityNow locking + txn support.
 *
 * Kernel rule:
 * - Store stays inode/direntry-centric and agnostic of locking.
 * - Store derives scope keys (PathKeyCodec) and persists them on vn_inode.scope_key.
 *
 * Store schema assumptions (confirmed):
 * - vn_inode(id BIGINT PK, created_at ...)
 * - vn_dir_entry(id BIGINT PK, parent_id BIGINT FK, child_id BIGINT FK, name TEXT, created_at ...)
 * - Paths are immutable once created (no re-parent, no rename, no subtree moves).
 *
 * Note: ltree extension must already be installed (via DBPostgresExtensionSupport).
 */
public final class DBPostgresLockingSupport extends AbstractAuxiliaryDatabaseObject {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger();


	// Fence token allocator
	private static final String CREATE_FENCE_TOKEN_SEQUENCE = 
	"""
	CREATE SEQUENCE IF NOT EXISTS vn_fence_token_seq	
	""";

	// Lock group (one per acquisition batch)
	private static final String CREATE_LOCK_GROUP = 
	"""
	CREATE TABLE IF NOT EXISTS vn_lock_group (
		lock_group_id UUID PRIMARY KEY,
		owner_id      TEXT NOT NULL,
		fence_token   BIGINT  NULL,
		active        BOOLEAN NOT NULL DEFAULT TRUE,
		acquired_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
		released_at   TIMESTAMPTZ
	)	
	""";
	
	// Individual scope locks (exclusive subtree locks via ltree)
	private static final String CREATE_PATH_LOCK = 
	"""
	CREATE TABLE IF NOT EXISTS vn_path_lock (
        id            BIGSERIAL PRIMARY KEY,
        lock_group_id UUID NOT NULL REFERENCES vn_lock_group(lock_group_id),
        owner_id      TEXT NOT NULL,
        scope_key     LTREE NOT NULL,
        active        BOOLEAN NOT NULL DEFAULT TRUE,
        acquired_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
        released_at   TIMESTAMPTZ
    )
	""";
	
	private static final String CREATE_TXN_EPOCH = 
	"""
	CREATE TABLE IF NOT EXISTS vn_txn_epoch (
        txn_id        TEXT PRIMARY KEY,
        lock_group_id UUID,
        fence_token   BIGINT,
        status        TEXT NOT NULL,
        updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
    )	
    """;	
	
	private static final String[] CREATE_STRINGS =  {
	    CREATE_FENCE_TOKEN_SEQUENCE,
	    CREATE_LOCK_GROUP,
	    CREATE_PATH_LOCK,
	    CREATE_TXN_EPOCH,

	    // --------- Indexes (no coupling to store tables) ----------
	    "CREATE INDEX IF NOT EXISTS ix_vn_lock_group_owner_active " +
	    "ON vn_lock_group(owner_id, active)",

	    "CREATE INDEX IF NOT EXISTS ix_vn_path_lock_scope_key_gist " +
	    "ON vn_path_lock USING GIST(scope_key)",

	    "CREATE INDEX IF NOT EXISTS ix_vn_path_lock_owner_active " +
	    "ON vn_path_lock(owner_id, active)",

	    "CREATE INDEX IF NOT EXISTS ix_vn_path_lock_group " +
	    "ON vn_path_lock(lock_group_id)",

	    "CREATE INDEX IF NOT EXISTS ix_vn_txn_epoch_status " +
	    "ON vn_txn_epoch(status)"
	};

	private static final String[] DROP_STRINGS =  {

	    // Root pointer

	    // Locking indexes
	    "DROP INDEX IF EXISTS ix_vn_txn_epoch_status",
	    "DROP INDEX IF EXISTS ix_vn_path_lock_group",
	    "DROP INDEX IF EXISTS ix_vn_path_lock_owner_active",
	    "DROP INDEX IF EXISTS ix_vn_path_lock_scope_key_gist",
	    "DROP INDEX IF EXISTS ix_vn_lock_group_owner_active",

	    // Locking tables (dependents first)
	    "DROP TABLE IF EXISTS vn_path_lock",
	    "DROP TABLE IF EXISTS vn_txn_epoch",
	    "DROP TABLE IF EXISTS vn_lock_group",

	    // Fence token allocator
	    "DROP SEQUENCE IF EXISTS vn_fence_token_seq"
	};

	
	@Override
	public String[] sqlCreateStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tCREATING Auxillary Tables for Locking Support");
		return CREATE_STRINGS;
	}

	@Override
	public String[] sqlDropStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tDROPPING Auxillary Tables created by Locking Support");
		return DROP_STRINGS;
	}

	@Override
	public boolean appliesToDialect(Dialect dialect) {
		return dialect instanceof PostgreSQLDialect;
	}

	@Override
	public boolean beforeTablesOnCreation() {
		// Must run after store entity tables exist, because we reference vn_inode/vn_dir_entry.
		return false;
	}
}
