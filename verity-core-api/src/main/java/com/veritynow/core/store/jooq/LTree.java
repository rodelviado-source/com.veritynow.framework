package com.veritynow.core.store.jooq;

import java.util.Objects;

/**
 * Strongly-typed representation of a PostgreSQL ltree label path.
 *
 * This is intentionally thin: invariants are enforced by the database and your PathKeyCodec.
 */
public record LTree(String value) {
  public LTree {
    Objects.requireNonNull(value, "value");
  }

  @Override
  public String toString() {
    return value;
  }

  public static LTree of(String value) {
    return new LTree(value);
  }
}
