package com.veritynow.core.store.tools.schema;

import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Constraint;
import org.jooq.DDLExportConfiguration;
import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;

/**
 * jOOQ-first DDL generator for an idempotent "create-if-absent" variant.
 *
 * <p>
 * This intentionally does not try to be a general migration engine. It
 * generates deterministic DDL purely from the jOOQ-generated schema model
 * (Public, Keys, Indexes, Sequences) without parsing SQL strings.
 * </p>
 */
public final class DDLCreatelfAbsent {

	public static void main(String[] args) {
		for (String s : generate())
			System.out.println(s);
	}

	public static List<String> generate() {
		List<String> out = new ArrayList<>();

		DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
		DDLExportConfiguration c =
				new DDLExportConfiguration().
				createSchemaIfNotExists(true).
				createTableIfNotExists(true).
				createIndexIfNotExists(true).
				createSequenceIfNotExists(true);

		dsl.ddl(PUBLIC, c).queryStream().forEach(s -> {
			if (s.getClass().getName().toLowerCase().contains("altertable")) {
				// skip
			} else {
				out.add(s.getSQL());
			}
		});

		for (UniqueKey<?> uk : PUBLIC.getUniqueKeys()) {
			//already created by Index
			if (uk.isPrimary()) continue;
			//already created
			if (uk.enforced()) continue;
			Table<?> table = uk.getTable();
			Constraint cons = uk.constraint();
			String o = dsl.begin(
					dsl.query("{0}; exception when duplicate_table then when duplicate_object then null",
							dsl.alterTableIfExists(table).add(cons)))
					.getSQL();
			out.add(o);
		}

		for (ForeignKey<?, ?> fk : PUBLIC.getForeignKeys()) {
			Table<?> table = fk.getTable();
			Constraint cons = fk.constraint();
			out.add(dsl.begin(
					dsl.query("{0}; exception when duplicate_table then  when duplicate_object then null",
							dsl.alterTableIfExists(table).add(cons)))
					.getSQL());
		}
		return out;
	}
}
