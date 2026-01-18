package com.veritynow.v2.store.core.jpa;

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
 * - Locking derives scope keys inside Postgres and maintains a projection table.
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

	// --------- Root pointer + deterministic root seeding (no id=1 assumption) ----------
	private static final String CREATE_VN_ROOT = 
	"""
	CREATE TABLE IF NOT EXISTS vn_root (
	  singleton BOOLEAN PRIMARY KEY DEFAULT TRUE,
	  inode_id  BIGINT NOT NULL REFERENCES vn_inode(id)
	)
	""";

	private static final String ENSURE_ROOT = 
	"""
	WITH ins_inode AS (
	  INSERT INTO vn_inode (created_at)
	  SELECT NOW()
	  WHERE NOT EXISTS (SELECT 1 FROM vn_root WHERE singleton = TRUE)
	  RETURNING id
	)
	INSERT INTO vn_root (singleton, inode_id)
	SELECT TRUE, id FROM ins_inode
	ON CONFLICT (singleton) DO NOTHING
	""";

	// Seed vn_scope_index with the actual root inode id -> '/'
	private static final String ENSURE_SCOPE_ROOT_FROM_VN_ROOT = 
	"""
	INSERT INTO vn_scope_index(inode_id, path_text, scope_key)
	SELECT r.inode_id, '/', vn_path_to_scope_key('/')
	FROM vn_root r
	WHERE r.singleton = TRUE
	ON CONFLICT (inode_id) DO NOTHING
	""";

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
	
	// --------- Scope key derivation (Postgres-authoritative) ----------
	private static final String CREATE_FUNC_PATH_TO_LTREE = 
	"""
	CREATE OR REPLACE FUNCTION vn_path_to_scope_key(p_path TEXT) 
	    RETURNS LTREE 
	    LANGUAGE plpgsql 
	    IMMUTABLE 
	    AS $$ 
	    DECLARE 
	      seg TEXT; 
	      out TEXT := ''; 
	    BEGIN 
	      FOREACH seg IN ARRAY regexp_split_to_array(p_path, '/') 
	      LOOP 
	        IF seg <> '' THEN 
	          out := out 
	            || CASE WHEN out = '' THEN '' ELSE '.' END 
	            || 'h' 
	            || substr(md5(seg), 1, 16); 
	        END IF; 
	      END LOOP; 
	      RETURN out::ltree; 
	    END; 
	    $$
    """;
	
	// --------- Locking-owned projection (inode_id -> absolute path + scope_key) ----------
	private static final String CREATE_SCOPE_INDEX = 
	""" 
	CREATE TABLE IF NOT EXISTS vn_scope_index (
        inode_id  BIGINT PRIMARY KEY,
        path_text TEXT NOT NULL,
        scope_key LTREE NOT NULL
    )
	""";
	
	private static final String CREATE_SCOPE_INDEX_SCOPEKEY_GIST = 
	""" 
	CREATE INDEX IF NOT EXISTS ix_vn_scope_index_scope_key_gist 
	ON vn_scope_index USING GIST (scope_key)
	""";
	
	private static final String CREATE_FUNC_ON_DIRENTRY_INSERT_PROJECT_TO_SCOPE_INDEX = 
	""" 
	CREATE OR REPLACE FUNCTION vn_scope_index_on_dir_entry_insert() 
    RETURNS trigger 
    LANGUAGE plpgsql 
    AS $$ 
    DECLARE 
      parent_path TEXT; 
      child_path  TEXT; 
    BEGIN 
      SELECT path_text INTO parent_path 
      FROM vn_scope_index 
      WHERE inode_id = NEW.parent_id; 
      IF parent_path IS NULL THEN 
        RAISE EXCEPTION 'vn_scope_index missing parent id=%', NEW.parent_id; 
      END IF; 
      IF parent_path = '/' THEN 
        child_path := '/' || NEW.name; 
      ELSE 
        child_path := parent_path || '/' || NEW.name; 
      END IF; 
      INSERT INTO vn_scope_index(inode_id, path_text, scope_key) 
      VALUES (NEW.child_id, child_path, vn_path_to_scope_key(child_path)) 
      ON CONFLICT (inode_id) DO NOTHING; 
      RETURN NEW; 
    END; 
    $$
	""";
	
	private static final String CREATE_TRIGGER_ON_DIRENTRY_INSERT = 
	""" 
	CREATE OR REPLACE TRIGGER trg_vn_dir_entry_scope_index_insert
    AFTER INSERT ON vn_dir_entry
    FOR EACH ROW
    EXECUTE FUNCTION vn_scope_index_on_dir_entry_insert()
	""";
	private static final String[] CREATE_STRINGS =  {
	    CREATE_FENCE_TOKEN_SEQUENCE,
	    CREATE_LOCK_GROUP,
	    CREATE_PATH_LOCK,
	    CREATE_TXN_EPOCH,
	    CREATE_FUNC_PATH_TO_LTREE,
	    CREATE_SCOPE_INDEX,
	    CREATE_SCOPE_INDEX_SCOPEKEY_GIST,
	    CREATE_VN_ROOT,
	    ENSURE_ROOT,
	    ENSURE_SCOPE_ROOT_FROM_VN_ROOT,
	    CREATE_FUNC_ON_DIRENTRY_INSERT_PROJECT_TO_SCOPE_INDEX,
	    CREATE_TRIGGER_ON_DIRENTRY_INSERT,

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

	// DROP-IN replacement for DROP_STRINGS (adds vn_root and fixes vn_txn_epoch table drop)
	private static final String[] DROP_STRINGS =  {
	    // Drop trigger then function
	    "DROP TRIGGER IF EXISTS trg_vn_dir_entry_scope_index_insert ON vn_dir_entry",
	    "DROP FUNCTION IF EXISTS vn_scope_index_on_dir_entry_insert()",

	    // Derivation function
	    "DROP FUNCTION IF EXISTS vn_path_to_scope_key(TEXT)",

	    // Projection index/table
	    "DROP INDEX IF EXISTS ix_vn_scope_index_scope_key_gist",
	    "DROP TABLE IF EXISTS vn_scope_index",

	    // Root pointer
	    "DROP TABLE IF EXISTS vn_root",

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
