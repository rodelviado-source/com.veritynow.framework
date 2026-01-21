package com.veritynow.core.store.jooq;

import org.jooq.Binding;
import org.jooq.Converter;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PGobject;

import java.sql.*;

/**
 * jOOQ Binding for PostgreSQL ltree.
 *
 * Database type is treated as JDBC Object (typically PGobject with type "ltree"),
 * and user type is {@link LTree}.
 *
 * SQL rendering always casts the bind parameter or literal to ::ltree to ensure operator correctness.
 */
public final class LTreeBinding implements Binding<Object, LTree> {

  private static final long serialVersionUID = 1L;

  private static final Converter<Object, LTree> CONVERTER = new Converter<>() {
    private static final long serialVersionUID = 1L;

    @Override
    public LTree from(Object databaseObject) {
      if (databaseObject == null) return null;

      if (databaseObject instanceof PGobject pg) {
        String v = pg.getValue();
        return (v == null) ? null : new LTree(v);
      }

      // Some drivers / queries may deliver String directly
      if (databaseObject instanceof String s) {
        return new LTree(s);
      }

      return new LTree(databaseObject.toString());
    }

    @Override
    public Object to(LTree userObject) {
      return userObject == null ? null : userObject.value();
    }

    @Override
    public Class<Object> fromType() {
      return Object.class;
    }

    @Override
    public Class<LTree> toType() {
      return LTree.class;
    }
  };

  @Override
  public Converter<Object, LTree> converter() {
    return CONVERTER;
  }

  @Override
  public void sql(org.jooq.BindingSQLContext<LTree> ctx) throws SQLException {
    // Render as either a bind parameter or an inlined literal, always cast to ltree.
    if (ctx.render().paramType() == ParamType.INLINED) {
      LTree v = ctx.value();
      if (v == null) {
        ctx.render().sql("null::ltree");
      } else {
        // Use jOOQ to properly escape the string literal, then cast
        ctx.render().visit(DSL.inline(v.value())).sql("::ltree");
      }
    } else {
      // Bind variable + cast
      ctx.render().sql("?::ltree");
    }
  }

  @Override
  public void register(org.jooq.BindingRegisterContext<LTree> ctx) throws SQLException {
    // For OUT parameters (rare for our use-case)
    ctx.statement().registerOutParameter(ctx.index(), Types.OTHER);
  }

  @Override
  public void set(org.jooq.BindingSetStatementContext<LTree> ctx) throws SQLException {
    PreparedStatement stmt = ctx.statement();
    int idx = ctx.index();
    LTree val = ctx.value();

    if (val == null) {
      stmt.setNull(idx, Types.OTHER);
      return;
    }

    PGobject obj = new PGobject();
    obj.setType("ltree");
    obj.setValue(val.value());
    stmt.setObject(idx, obj, Types.OTHER);
  }

  @Override
  public void set(org.jooq.BindingSetSQLOutputContext<LTree> ctx) throws SQLException {
    // Not used for Postgres in typical JDBC contexts
    throw new SQLFeatureNotSupportedException("SQLDataOutput not supported");
  }

  @Override
  public void get(org.jooq.BindingGetResultSetContext<LTree> ctx) throws SQLException {
    Object o = ctx.resultSet().getObject(ctx.index());
    ctx.value(converter().from(o));
  }

  @Override
  public void get(org.jooq.BindingGetStatementContext<LTree> ctx) throws SQLException {
    Object o = ctx.statement().getObject(ctx.index());
    ctx.value(converter().from(o));
  }

  @Override
  public void get(org.jooq.BindingGetSQLInputContext<LTree> ctx) throws SQLException {
    // Not used for Postgres in typical JDBC contexts
    throw new SQLFeatureNotSupportedException("SQLDataInput not supported");
  }
}
