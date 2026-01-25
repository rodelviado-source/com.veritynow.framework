package com.veritynow.core.store.tools.schema;

import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC;
import static org.jooq.impl.DSL.using;

import java.util.Arrays;
import java.util.List;

import org.jooq.Queries;
import org.jooq.SQLDialect;

/**
 * jOOQ-first DDL generator for an idempotent "create-if-absent" variant.
 *
 * <p>This intentionally does not try to be a general migration engine. It generates
 * deterministic DDL purely from the jOOQ-generated schema model (Public, Keys, Indexes,
 * Sequences) without parsing SQL strings.</p>
 */
public final class SQLCreate {
  
  /**
   * Generate idempotent DDL statements (no trailing semicolons) for the {@code public}
   * schema as modelled by jOOQ codegen.
   */
  public static List<String> generate() {
	// Baseline (strict) DDL is produced directly by jOOQ.
	Queries ddl = using(SQLDialect.POSTGRES).ddl(PUBLIC);
	return Arrays.asList(ddl.queries()).stream().map(q -> q.getSQL()).toList();
  }
}
