package com.veritynow.core.store.jooq;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Minimal, explicit helpers for PostgreSQL ltree operators.
 *
 * We keep these helpers small and transparent to preserve your "path-centric" intent:
 * the DB remains the source of truth for ltree semantics, and repositories stay readable.
 */
public final class LTreeDSL {

  private LTreeDSL() {}

  /**
   * a @> b  (a is ancestor-of-or-equal to b)
   */
  public static Condition contains(Field<LTree> a, Field<LTree> b) {
    return DSL.condition("{0} @> {1}", a, b);
  }

  public static Condition contains(Field<LTree> a, LTree b) {
    return DSL.condition("{0} @> {1}::ltree", a, DSL.val(b == null ? null : b.value()));
  }

  /**
   * a <@ b  (a is descendant-of-or-equal to b)
   */
  public static Condition isContainedBy(Field<LTree> a, Field<LTree> b) {
    return DSL.condition("{0} <@ {1}", a, b);
  }

  public static Condition isContainedBy(Field<LTree> a, LTree b) {
    return DSL.condition("{0} <@ {1}::ltree", a, DSL.val(b == null ? null : b.value()));
  }

  /**
   * ltree pattern match: a ~ b
   */
  public static Condition matches(Field<LTree> a, String lqueryPattern) {
    return DSL.condition("{0} ~ {1}::lquery", a, DSL.val(lqueryPattern));
  }

  /**
   * ltree text-query match: a ? b
   */
  public static Condition matchesText(Field<LTree> a, String ltxtqueryPattern) {
    return DSL.condition("{0} ? {1}::ltxtquery", a, DSL.val(ltxtqueryPattern));
  }

  /**
   * Cast a String to an ltree value expression.
   */
  public static Field<LTree> ltree(String path) {
    // Use explicit cast to ltree. Binding handles bind casting too; this is for ad-hoc expressions.
    return DSL.field("{0}::ltree", LTree.class, DSL.val(path));
  }
}
