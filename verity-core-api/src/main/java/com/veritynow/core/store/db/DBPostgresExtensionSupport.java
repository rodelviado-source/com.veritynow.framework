package com.veritynow.core.store.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

public class DBPostgresExtensionSupport extends AbstractAuxiliaryDatabaseObject{
	 private static final Logger LOGGER = LogManager.getLogger();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public String[] sqlCreateStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tCreating postgres ltree extension");
		return new String[] {
			"CREATE EXTENSION IF NOT EXISTS ltree",
		}; 
	}

	@Override
	public String[] sqlDropStrings(SqlStringGenerationContext context) {
		LOGGER.info("\n\tDropping postgres ltree extension");
		return new String[] {
			"DROP EXTENSION IF EXISTS ltree",
		}; 
	}

	@Override
	public boolean appliesToDialect(Dialect dialect) {
	  return dialect instanceof PostgreSQLDialect;
	}

	@Override
	public boolean beforeTablesOnCreation() {
		return true;
	}

	
	
}
