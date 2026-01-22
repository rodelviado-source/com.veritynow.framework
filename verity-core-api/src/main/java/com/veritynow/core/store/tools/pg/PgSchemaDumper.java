package com.veritynow.core.store.tools.pg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

public final class PgSchemaDumper {

  private PgSchemaDumper() {}

  /**
   * Dump schema-only DDL for a single schema (e.g. "public") into a Liquibase formatted SQL file.
   *
   * This is a JDBC-only "pg_dump --schema-only" approximation using pg_catalog and pg_get_*def helpers.
   */
  public static void dumpSchema(DataSource ds, String schema, Path outFile) throws SQLException, IOException {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(outFile, "outFile");

    Files.createDirectories(outFile.toAbsolutePath().getParent());

    try (Connection c = ds.getConnection()) {
      c.setAutoCommit(true);

      try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

        // Liquibase formatted SQL header
        w.write("--liquibase formatted sql\n\n");
        w.write("--changeset verity:001-init-schema failOnError:true runOnChange:false\n");
        w.write("--comment: Baseline schema captured via JDBC from Hibernate auto-DDL at " + Instant.now() + "\n\n");

        // Optional: search_path for consistent object resolution
        w.write("SET search_path = " + qIdent(schema) + ";\n\n");

        // 1) Extensions used by this schema (at least ltree for you)
        // If you want to be explicit, list known required extensions; otherwise introspect.
        // For VerityNow: ltree is critical.
        writeLine(w, "CREATE EXTENSION IF NOT EXISTS ltree;");

        // 2) Tables (CREATE TABLE)
        List<TableRef> tables = listTables(c, schema);

        // Ensure stable ordering (dependencies handled by constraints later)
        for (TableRef t : tables) {
          writeCreateTable(c, w, schema, t.tableName());
          w.write("\n");
        }

        // 3) Constraints (PK/UK/FK/CHECK) – add after tables
        for (TableRef t : tables) {
          writeConstraints(c, w, schema, t.tableName());
        }
        w.write("\n");

        // 4) Indexes (including GIST)
        writeIndexes(c, w, schema);

        // 5) Sequences (if you use explicit sequences)
        writeSequences(c, w, schema);

        // 6) Views (optional)
        writeViews(c, w, schema);

        // 7) Triggers (optional)
        writeTriggers(c, w, schema);

        w.write("\n--rollback: (baseline) no rollback\n");
      }
    }
  }

  private static void writeCreateTable(Connection c, BufferedWriter w, String schema, String table) throws SQLException, IOException {
    // Build a CREATE TABLE statement using information_schema.columns for column definitions.
    // This avoids requiring pg_dump, and works reliably for common types (including "ltree").
    String sql =
        "select column_name, data_type, udt_name, is_nullable, column_default, " +
        "       character_maximum_length, numeric_precision, numeric_scale, datetime_precision " +
        "from information_schema.columns " +
        "where table_schema = ? and table_name = ? " +
        "order by ordinal_position";

    List<String> lines = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String col = rs.getString("column_name");
          String dataType = rs.getString("data_type");
          String udt = rs.getString("udt_name");

          String typeSql = toTypeSql(dataType, udt, rs);

          String nullable = "YES".equalsIgnoreCase(rs.getString("is_nullable")) ? "" : " NOT NULL";
          String def = rs.getString("column_default");
          String defaultSql = (def == null) ? "" : " DEFAULT " + def;

          lines.add("  " + qIdent(col) + " " + typeSql + defaultSql + nullable);
        }
      }
    }

    writeLine(w, "CREATE TABLE IF NOT EXISTS " + qIdent(schema) + "." + qIdent(table) + " (\n" +
        String.join(",\n", lines) + "\n);");
  }

  private static void writeConstraints(Connection c, BufferedWriter w, String schema, String table) throws SQLException, IOException {
    String sql =
        "select conname, pg_get_constraintdef(c.oid, true) as def " +
        "from pg_constraint c " +
        "join pg_class t on t.oid = c.conrelid " +
        "join pg_namespace n on n.oid = t.relnamespace " +
        "where n.nspname = ? and t.relname = ? " +
        "order by case c.contype when 'p' then 1 when 'u' then 2 when 'f' then 3 when 'c' then 4 else 9 end, conname";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String con = rs.getString("conname");
          String def = rs.getString("def");
          writeLine(w, "ALTER TABLE IF EXISTS " + qIdent(schema) + "." + qIdent(table) +
              " ADD CONSTRAINT " + qIdent(con) + " " + def + ";");
        }
      }
    }
  }

  private static void writeIndexes(Connection c, BufferedWriter w, String schema) throws SQLException, IOException {
    String sql =
        "select idx.relname as index_name, tbl.relname as table_name, pg_get_indexdef(i.indexrelid) as def " +
        "from pg_index i " +
        "join pg_class idx on idx.oid = i.indexrelid " +
        "join pg_class tbl on tbl.oid = i.indrelid " +
        "join pg_namespace n on n.oid = tbl.relnamespace " +
        "where n.nspname = ? " +
        "  and not i.indisprimary " +          // PK indexes are implied by constraints, but harmless if included
        "order by tbl.relname, idx.relname";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String def = rs.getString("def");
          // pg_get_indexdef already includes "CREATE INDEX ..." with schema-qualified table
          writeLine(w, def + ";");
        }
      }
    }
  }

  private static void writeSequences(Connection c, BufferedWriter w, String schema) throws SQLException, IOException {
    // For modern identity/serial, sequences often already appear via defaults; but explicit sequences (like vn_fence_token_seq)
    // should be captured here.
    String sql =
        "select sequence_name, data_type, start_value, minimum_value, maximum_value, increment, cycle_option " +
        "from information_schema.sequences " +
        "where sequence_schema = ? " +
        "order by sequence_name";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("sequence_name");
          // Minimal CREATE SEQUENCE that is safe for baseline
          writeLine(w, "CREATE SEQUENCE IF NOT EXISTS " + qIdent(schema) + "." + qIdent(name) + ";");
        }
      }
    }
  }

  private static void writeViews(Connection c, BufferedWriter w, String schema) throws SQLException, IOException {
    String sql =
        "select table_name, view_definition " +
        "from information_schema.views " +
        "where table_schema = ? " +
        "order by table_name";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String view = rs.getString("table_name");
          String def = rs.getString("view_definition");
          writeLine(w, "CREATE OR REPLACE VIEW " + qIdent(schema) + "." + qIdent(view) + " AS\n" + def + ";");
        }
      }
    }
  }

  private static void writeTriggers(Connection c, BufferedWriter w, String schema) throws SQLException, IOException {
    String sql =
        "select t.tgname, tbl.relname as table_name, pg_get_triggerdef(t.oid, true) as def " +
        "from pg_trigger t " +
        "join pg_class tbl on tbl.oid = t.tgrelid " +
        "join pg_namespace n on n.oid = tbl.relnamespace " +
        "where n.nspname = ? and not t.tgisinternal " +
        "order by tbl.relname, t.tgname";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String def = rs.getString("def");
          // pg_get_triggerdef returns "CREATE TRIGGER ..." but may omit IF NOT EXISTS; baseline is fine
          writeLine(w, def + ";");
        }
      }
    }
  }

  private static List<TableRef> listTables(Connection c, String schema) throws SQLException {
    String sql =
        "select table_name " +
        "from information_schema.tables " +
        "where table_schema = ? and table_type = 'BASE TABLE' " +
        "order by table_name";
    List<TableRef> out = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new TableRef(schema, rs.getString(1)));
        }
      }
    }
    return out;
  }

  private record TableRef(String schema, String tableName) {}

  private static String toTypeSql(String dataType, String udt, ResultSet rs) throws SQLException {
    // Handle user-defined types like ltree: information_schema reports data_type='USER-DEFINED', udt_name='ltree'
    if ("USER-DEFINED".equalsIgnoreCase(dataType)) {
      return qIdent(udt);
    }

    // Handle common typed-length
    if ("character varying".equalsIgnoreCase(dataType) || "varchar".equalsIgnoreCase(dataType)) {
      Integer len = (Integer) rs.getObject("character_maximum_length");
      return (len == null) ? "varchar" : "varchar(" + len + ")";
    }
    if ("character".equalsIgnoreCase(dataType) || "char".equalsIgnoreCase(dataType)) {
      Integer len = (Integer) rs.getObject("character_maximum_length");
      return (len == null) ? "char" : "char(" + len + ")";
    }
    if ("numeric".equalsIgnoreCase(dataType) || "decimal".equalsIgnoreCase(dataType)) {
      Integer p = (Integer) rs.getObject("numeric_precision");
      Integer s = (Integer) rs.getObject("numeric_scale");
      if (p != null && s != null) return "numeric(" + p + "," + s + ")";
      if (p != null) return "numeric(" + p + ")";
      return "numeric";
    }

    // default: use information_schema's type label
    return dataType;
  }

  private static void writeLine(BufferedWriter w, String s) throws IOException {
    w.write(s);
    if (!s.endsWith("\n")) w.write("\n");
  }

  private static String qIdent(String ident) {
    // minimal identifier quoting; safe for snake_case and mixed case.
    return "\"" + ident.replace("\"", "\"\"") + "\"";
  }
}
