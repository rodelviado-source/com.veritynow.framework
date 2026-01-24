package com.veritynow.core.store.tools.pg;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, deterministic SQL rewriter to generate an idempotent ("drop-if-exists")
 * variant of captured PostgreSQL DDL.
 *
 * <p>Important: This is intentionally NOT a general-purpose migration engine.
 * It only makes DROP/teardown DDL safe to re-run by adding IF EXISTS where supported and
 * by wrapping DROP CONSTRAINT statements to ignore undefined_object errors.</p>
 */
public final class SchemaToDropIfExist {
	
	private static final String QIDENT = "\"[^\"]+\"";
	private static final String QUALIFIED_QIDENT = QIDENT + "(?:\\s*\\.\\s*" + QIDENT + ")?";

	private static final Pattern CREATE_TABLE =
		    Pattern.compile(
		            "^\\s*create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?" +
		            "(?<name>" + QUALIFIED_QIDENT + ")\\s*\\(",
		            Pattern.CASE_INSENSITIVE
		        );
	  
	private static final Pattern CREATE_INDEX =
		    Pattern.compile(
		        "^\\s*create\\s+(?:unique\\s+)?index\\s+" +
		        "(?:if\\s+not\\s+exists\\s+)?" +
		        "(?<name>" + QUALIFIED_QIDENT + ")\\s+on\\s+",
		        Pattern.CASE_INSENSITIVE
		    );
	  
	private static final Pattern CREATE_SEQUENCE =
		    Pattern.compile(
		        "^\\s*create\\s+sequence\\s+" +
		        "(?:if\\s+not\\s+exists\\s+)?" +
		        "(?<name>" + QUALIFIED_QIDENT + ")",
		        Pattern.CASE_INSENSITIVE
		    );

	  //alter table "public"."vn_dir_entry" add constraint "fkphtn4icdtoh8xgjuvtw4qcd0i"
	  //ALTER TABLE if exist table_name DROP CONSTRAINT constraint_name;
//	  private static final Pattern ALTER_ADD_CONSTRAINT =
//	      //Pattern.compile("^\s*alter\s+table\s+.+\s+add\s+constraint\s+.+$", Pattern.CASE_INSENSITIVE);
//	     Pattern.compile("^\\s*alter\\s+table\\s+(\".*\")\\s+add\\s+constraint\\s+(\".*?\")\\s+", Pattern.CASE_INSENSITIVE);	  
  
	private static final Pattern ALTER_ADD_CONSTRAINT =
		    Pattern.compile(
		        "^\\s*alter\\s+table\\s+(?<table>" + QUALIFIED_QIDENT + ")\\s+" +
		        "add\\s+constraint\\s+(?<cname>" + QIDENT + ")\\s+(?<rest>.+?)\\s*;?\\s*$",
		        Pattern.CASE_INSENSITIVE
		    );
	
	
  private SchemaToDropIfExist() {}

  public static String rewrite(String sql) {
    if (sql == null) return "";
    String trimmed = sql.trim();
    if (trimmed.isEmpty()) return trimmed;

    String lower = trimmed.toLowerCase(Locale.ROOT);

    // Already idempotent? Leave it.
    if (lower.contains(" if exists ")) return trimmed;

    Matcher m = CREATE_TABLE.matcher(trimmed);
    if (m.find()) return "drop table if exists " + m.group("name");
    
    if (trimmed.contains("create table")) {
    	System.out.println("syntax error table identifier not found " + trimmed);
    }
    
    m = CREATE_INDEX.matcher(trimmed);
    if (m.find()) return "drop index if exists " + m.group("name");
    
    if (trimmed.contains("create index")) {
    	System.out.println("syntax error index identifier not found " + trimmed);
    }
    
    // DROP TABLE
//    Matcher m = CREATE_TABLE.matcher(trimmed);
//    boolean matched = m.find();
//   	String identifier = matched ? m.group(1) : null;
//    if (identifier != null) {
//        System.out.println("Captured: " + identifier);
//        return "drop table if exists " + identifier;
//    } else if (matched){
//    	System.out.println("syntax error table identifier not found " + trimmed);
//    	return "";
//    }	
    
    
//    m = CREATE_INDEX.matcher(trimmed);
//    boolean matched = m.find();
//   	String identifier = matched ? m.group(1) : null;
//    if (identifier != null) {
//        System.out.println("Captured: " + identifier);
//        return "drop index if exists " + identifier;
//    } else if (matched){
//    	System.out.println("syntax error index identifier not found " + trimmed);
//    	return "";
//    }	

//    m = ALTER_ADD_CONSTRAINT.matcher(trimmed);
//    boolean matched = m.find();
//   	String tableName = matched ? m.group(1) : null;
//   	String constraintName = matched ? m.group(2) : null;
//    if (tableName != null && constraintName != null) {
//        System.out.println("Captured: " + tableName + " " + constraintName);
//        return "alter table if exists " + tableName + " drop constraint if exists " + constraintName;
//    } else if (matched){
//    	if (tableName == null)
//    		System.out.println("syntax error table identifier name not found");
//    	if (constraintName == null)
//    		System.out.println("syntax error constraint identifier not found");
//    	System.out.println(trimmed);
//    	return "";
//    }	
    
    m = ALTER_ADD_CONSTRAINT.matcher(trimmed);
    if (m.find()) return "alter table if exists " + m.group("table") + " drop constraint if exists " + m.group("cname");
    
    if (trimmed.contains("alter table")) {
    	System.out.println("syntax error table and/or constraint identifier not found " + trimmed);
    }
    
    
    m = CREATE_SEQUENCE.matcher(trimmed);
    if (m.find()) return "drop sequence if exists " + m.group("name");
    
    if (trimmed.contains("create sequence")) {
    	System.out.println("syntax error sequence identifier not found " + trimmed);
    }
    
//    m = CREATE_SEQUENCE.matcher(trimmed);
//    matched = m.find();
//   	String identifier = matched ? m.group(1) : null;
//    if (identifier != null) {
//        System.out.println("Captured: " + identifier);
//        return "drop sequence if exists " + identifier;
//    } else if (matched){
//    	System.out.println("syntax error sequence identifier not found " + trimmed);
//    	return "";
//    }	
    


    // Default: leave as-is.
    return trimmed;
  }
}
