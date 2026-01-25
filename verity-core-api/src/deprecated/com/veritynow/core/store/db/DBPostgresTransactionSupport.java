package com.veritynow.core.store.db;

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
public final class DBPostgresTransactionSupport extends AbstractAuxiliaryDatabaseObject {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String[] CREATE_STRINGS =  {
			 "ALTER TABLE IF EXISTS vn_node_head " +
		     "ADD COLUMN IF NOT EXISTS fence_token BIGINT"
	};
	
	private static final String[] DROP_STRINGS =  {
			"ALTER TABLE IF EXISTS vn_node_head " +
			"DROP COLUMN IF EXISTS fence_token"
		};
	
	@Override
	public String[] sqlCreateStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tAdding fence_token in vn_inode_head for Transaction Support");
		return CREATE_STRINGS;
	}

	@Override
	public String[] sqlDropStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tDROPPING fence_token in  vn_inode_head created by Transaction Support");
		return DROP_STRINGS;
	}

	@Override
	public boolean appliesToDialect(Dialect dialect) {
		return dialect instanceof PostgreSQLDialect;
	}

	@Override
	public boolean beforeTablesOnCreation() {
		// Must run after store entity tables exist, because we reference vn_node_head.
		return false;
	}
}
