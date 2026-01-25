package com.veritynow.core.store.tools.schema;

import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jooq.Constraint;
import org.jooq.DDLExportConfiguration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.Sequence;
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
public final class SQLCreatelfAbsent {

	private static DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

	/**
	 * Generate idempotent DDL statements (no trailing semicolons) for the
	 * {@code public} schema as modelled by jOOQ codegen.
	 */
	public static List<String> generate2() {

		// Sort tables by name for stable output
		Map<String, Table<?>> tables = new TreeMap<>();
		PUBLIC.getTables().stream().forEach(t -> tables.put(t.getQualifiedName().toString(), t));

		// 0) Initialize indexes in a sorted map
		Map<String, Index> indexes = new TreeMap<String, Index>();
		for (Table<?> t : tables.values()) {
			t.getIndexes().stream().forEach(i -> indexes.put(i.getQualifiedName().toString(), i));
		}

		List<String> out = new ArrayList<>();

		// 2) Tables (IF NOT EXISTS) – columns only. Constraints are emitted as ALTER
		// TABLE.
		for (Table<?> t : tables.values()) {
			// Build CREATE TABLE IF NOT EXISTS ... (columns...)
			var step = dsl.createTableIfNotExists(t);
			for (Field<?> f : t.fields())
				step = step.column(f);
			out.add(step.getSQL());
		}

		// 3) Constraints – emit in deterministic order. For idempotence without
		// procedural SQL,
		// we use: ALTER TABLE IF EXISTS <t> DROP CONSTRAINT IF EXISTS <c>, ADD
		// CONSTRAINT <c> ...

		for (Table<?> t : tables.values()) {
			UniqueKey<?> pk = t.getPrimaryKey();
			if (pk != null) {
				out.add(renderDropAddConstraint(t, pk.constraint()));
			}
		}

		for (Table<?> t : tables.values()) {
			UniqueKey<?> pk = t.getPrimaryKey();

			for (UniqueKey<?> uk : t.getKeys()) {
				if (pk != null && uk.getName().equals(pk.getName()))
					continue;
				out.add(renderDropAddConstraint(t, uk.constraint()));
			}
		}
		for (Table<?> t : tables.values()) {
			for (ForeignKey<?, ?> fk : t.getReferences()) {
				out.add(renderDropAddConstraint(t, fk.constraint()));
			}
		}

		// 4) Indexes (IF NOT EXISTS)
		for (Index idx : indexes.values()) {
			out.add(renderCreateIndexIfNotExists(idx));
		}

		// 1) Sequences (IF NOT EXISTS)
		for (Sequence<?> seq : PUBLIC.getSequences()) {
			Query q = dsl.createSequenceIfNotExists(seq);
			out.add(q.getSQL());
		}

		return out;
	}

	private static String renderDropAddConstraint(Table<?> table, Constraint add) {

		// Build an ALTER TABLE with comma-separated drop/add.
		Query q = dsl.query(dsl.alterTableIfExists(table).dropConstraintIfExists(add).cascade().getSQL() + ", add {0}",
				add);
		return q.getSQL();
	}

	private static String renderCreateIndexIfNotExists(Index idx) {
		Table<?> table = idx.getTable();
		if (idx.getUnique()) {
			return dsl.createUniqueIndexIfNotExists(idx).on(table, table.fields()).getSQL();
		}
		return dsl.createIndexIfNotExists(idx).on(table, table.fields()).getSQL();
	}

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
			if (uk.isPrimary()) continue;
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
