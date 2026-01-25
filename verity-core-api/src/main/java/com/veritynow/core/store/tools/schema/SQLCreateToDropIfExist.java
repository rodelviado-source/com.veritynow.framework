package com.veritynow.core.store.tools.schema;

import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;

/**
 * jOOQ-first DDL generator for an idempotent "drop-if-exists" teardown variant.
 *
 * <p>Generates deterministic DROP statements purely from the jOOQ-generated schema model
 * without parsing SQL strings.</p>
 */
public final class SQLCreateToDropIfExist {

  private static DSLContext dsl = DSL.using(SQLDialect.POSTGRES);	
  private SQLCreateToDropIfExist() {}

  /**
   * Generate teardown DDL statements (no trailing semicolons) for the {@code public} schema.
   *
   * <p>Ordering is:
   * <ol>
   *   <li>Constraints</li>
   *   <li>Indexes</li>
   *   <li>Tables</li>
   *   <li>Sequences</li>
   * </ol>
   * This prevents dependency errors during teardown.</p>
   */
  public static List<String> generate() {
    List<String> out = new ArrayList<>();

    // Stable ordering of tables for consistent output
    List<Table<?>> tables = new ArrayList<>(PUBLIC.getTables());
    tables.sort(Comparator.comparing(t -> t.getQualifiedName().toString()));

    Map<String, Index> indexes = new TreeMap<String, Index>();
     
    // 1) Constraints (drop)
    for (Table<?> t : tables) {
      t.getIndexes().stream().forEach(i -> indexes.put(i.getName(), i));	
      // Primary key and unique keys
      for (UniqueKey<?> uk : sortedUniqueKeys(t)) {
        if (uk.getName() != null) {
          Query q = dsl.alterTableIfExists(t)
              .dropConstraintIfExists(uk.constraint()).cascade();
          out.add(q.getSQL());
        }
      }

      // Foreign keys
      for (ForeignKey<?, ?> fk : sortedForeignKeys(t)) {
        if (fk.getName() != null) {
          Query q = dsl.alterTableIfExists(t)
              .dropConstraintIfExists(fk.constraint()).cascade();
          out.add(q.getSQL());
        }
      }
    }

    // 2) Indexes (drop)
    for (Index idx : indexes.values()) {
      Name idxName = idx.getQualifiedName() != null ? idx.getQualifiedName() : idx.getUnqualifiedName();
      if (idxName == null) continue;
      out.add(dsl.dropIndexIfExists(idx).cascade().getSQL());
    }

    // 3) Tables (drop)
    // Reverse table order for teardown safety
    tables.sort(Comparator.comparing((Table<?> t) -> t.getQualifiedName().toString()).reversed());
    for (Table<?> t : tables) {
      out.add(dsl.dropTableIfExists(t).cascade().getSQL());
    }

 // 4) Sequences (drop)
    ArrayList<Sequence<?>> seqs = new ArrayList<Sequence<?>>(PUBLIC.getSequences());
    seqs.sort( Comparator.comparing((Sequence<?> s)  -> s.getQualifiedName().toString()).reversed());
    for (Sequence<?> seq : seqs) {
      out.add(dsl.dropSequenceIfExists(seq).getSQL());
    }

    return out;
  }

  private static List<UniqueKey<?>> sortedUniqueKeys(Table<?> t) {
    List<UniqueKey<?>> keys = new ArrayList<>();
    if (t.getPrimaryKey() != null) keys.add(t.getPrimaryKey());
    keys.addAll(t.getKeys());

    keys.sort(Comparator.comparing(k -> k.getName() == null ? "" : k.getName()));

    // Remove duplicates (primary key may also appear in getKeys())
    List<UniqueKey<?>> dedup = new ArrayList<>();
    for (UniqueKey<?> k : keys) {
      boolean exists = dedup.stream().anyMatch(x -> safeName(x.getName()).equals(safeName(k.getName())));
      if (!exists) dedup.add(k);
    }
    return dedup;
  }

  private static List<ForeignKey<?, ?>> sortedForeignKeys(Table<?> t) {
    List<ForeignKey<?, ?>> fks = new ArrayList<>(t.getReferences());
    fks.sort(Comparator.comparing(k -> k.getName() == null ? "" : k.getName()));
    return fks;
  }

  private static String safeName(String s) {
    return s == null ? "" : s;
  }
}
