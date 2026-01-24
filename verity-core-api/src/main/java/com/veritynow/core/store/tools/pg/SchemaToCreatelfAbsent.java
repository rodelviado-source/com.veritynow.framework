package com.veritynow.core.store.tools.pg;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, deterministic SQL rewriter to generate an idempotent ("create-if-absent")
 * variant of the captured PostgreSQL DDL.
 *
 * <p>Important: This is intentionally NOT a general-purpose migration engine.
 * It only makes DDL safe to re-run by adding IF NOT EXISTS where supported and
 * by wrapping ADD CONSTRAINT statements to ignore duplicate_object errors.</p>
 */
public final class SchemaToCreatelfAbsent {

	private static final String QIDENT = "\"[^\"]+\"";
	private static final String QUALIFIED_QIDENT = QIDENT + "(?:\\s*\\.\\s*" + QIDENT + ")?";
	
	private static final Pattern CREATE_TABLE = Pattern.compile("^\\s*create\\s+table\\s+", Pattern.CASE_INSENSITIVE);

	private static final Pattern CREATE_UNIQUE_INDEX = Pattern.compile("^\\s*create\\s+unique\\s+index\\s+",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern CREATE_INDEX = Pattern.compile("^\\s*create\\s+index\\s+", Pattern.CASE_INSENSITIVE);

	private static final Pattern CREATE_SEQUENCE = Pattern.compile("^\\s*create\\s+sequence\\s+",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ALTER_ADD_CONSTRAINT =
		    Pattern.compile(
		        "^\\s*alter\\s+table\\s+(?<table>" + QUALIFIED_QIDENT + ")\\s+" +
		        "add\\s+constraint\\s+(?<cname>" + QIDENT + ")\\s+(?<rest>.+?)\\s*;?\\s*$",
		        Pattern.CASE_INSENSITIVE
		    );

  private SchemaToCreatelfAbsent() {}

  public static String rewrite(String sql) {
    if (sql == null) return "";
    String trimmed = sql.trim();
    if (trimmed.isEmpty()) return trimmed;

    String lower = trimmed.toLowerCase(Locale.ROOT);

    // Already idempotent? Leave it.
    if (lower.contains(" if not exists ")) return trimmed;

    // CREATE TABLE
    if (CREATE_TABLE.matcher(trimmed).find()) {
      return CREATE_TABLE.matcher(trimmed).replaceFirst("create table if not exists ");
    }

    // CREATE UNIQUE INDEX
    if (CREATE_UNIQUE_INDEX.matcher(trimmed).find()) {
      return CREATE_UNIQUE_INDEX.matcher(trimmed).replaceFirst("create unique index if not exists ");
    }

    // CREATE INDEX
    if (CREATE_INDEX.matcher(trimmed).find()) {
      return CREATE_INDEX.matcher(trimmed).replaceFirst("create index if not exists ");
    }

    // CREATE SEQUENCE
    if (CREATE_SEQUENCE.matcher(trimmed).find()) {
      return CREATE_SEQUENCE.matcher(trimmed).replaceFirst("create sequence if not exists ");
    }

    
    Matcher m = ALTER_ADD_CONSTRAINT.matcher(trimmed);
    if (m.matches()) {
      String table = m.group("table");
      String cname = m.group("cname");
      String rest  = m.group("rest");

      return "alter table if exists " + table
          + " drop constraint if exists " + cname
          + ", add constraint " + cname + " " + rest;
    }
    
    
    // ALTER TABLE ... ADD CONSTRAINT ...
//    Matcher m = ALTER_ADD_CONSTRAINT.matcher(trimmed);
//    boolean matched = m.find();
//   	String tableName = matched ? m.group(1) : null;
//   	String constraintName = matched ? m.group(2) : null;
//   	String rest = matched ? m.group(3) : null;
//    if (tableName != null && constraintName != null && rest != null) {
//        System.out.println("Captured: " + tableName + " " + constraintName);
//        return "alter table if exists " + tableName + " add constraint if not exists " + constraintName + " " + rest;
//    } else if (matched){
//    	if (tableName == null)
//    		System.out.println("syntax error table identifier name not found");
//    	if (constraintName == null)
//    		System.out.println("syntax error constraint identifier not found");
//    	System.out.println(trimmed);
//    	return "";
//    }

    // Default: leave as-is.
    return trimmed;
  }
}
