package com.veritynow.v2.store.core.jpa;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class DBPostgresLtreeType implements UserType<String> {

  @Override public int getSqlType() { return Types.OTHER; }   // important: NOT VARCHAR
  @Override public Class<String> returnedClass() { return String.class; }

  @Override
  public boolean equals(String x, String y) {
    return x == null ? y == null : x.equals(y);
  }

  @Override
  public int hashCode(String x) { return x == null ? 0 : x.hashCode(); }

  @Override
  public String nullSafeGet(
      ResultSet rs, int position,
      SharedSessionContractImplementor session, Object owner) throws SQLException {

    Object v = rs.getObject(position);
    return v == null ? null : v.toString();
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, String value, int index,
      SharedSessionContractImplementor session) throws SQLException {

    if (value == null) {
      st.setNull(index, Types.OTHER);
    } else {
      // Postgres driver will treat Types.OTHER as "unknown", and it will align with ltree column type.
      // This prevents "expression is of type character varying" in many contexts.
      st.setObject(index, value, Types.OTHER);
    }
  }

  @Override public String deepCopy(String value) { return value; }
  @Override public boolean isMutable() { return false; }
  @Override public Serializable disassemble(String value) { return value; }
  @Override public String assemble(Serializable cached, Object owner) { return (String) cached; }
  @Override public String replace(String original, String target, Object owner) { return original; }
}
