package com.veritynow.core.store.tools.pg;

import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.jooq.DSLContext;
import org.jooq.Queries;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "verity.schema.dump.enabled", havingValue = "true")
public class SchemaDumpOnReady {

  private static final Pattern CREATE_SCHEMA = Pattern.compile("^\s*create\s+schema\s+", Pattern.CASE_INSENSITIVE);	
  
  private final DSLContext dsl;

  @Value("${verity.schema.dump.schema:public}")
  private String schema;

  /**
   * Comma-separated list of Postgres extensions to include in the generated SQL output.
   * Default includes ltree because the store relies on it.
   */
  @Value("${verity.schema.dump.extensions:ltree}")
  private String extensions;

  @Value("${verity.schema.dump.path:ddl}")
  private String outPath;

  public SchemaDumpOnReady(DSLContext dsl) {
    this.dsl = dsl;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {

    Queries ddl = dsl.ddl(PUBLIC);

    Path base = Path.of(outPath).resolve("schema-baseline");
    
    Path createPath = withSuffix(base, "-create.sql");
    Path createIfAbsentPath = withSuffix(base, "-create-if-absent.sql");
    Path dropIfExistsPath = withSuffix(base, "-drop-if-exists.sql");
    
    Files.createDirectories(base.toAbsolutePath().getParent());

    backupIfExists(createPath);
    backupIfExists(createIfAbsentPath);
    backupIfExists(dropIfExistsPath);

    String prelude = buildPrelude(schema, extensions);

    writeScript(createPath, ddl, prelude, "create");
    writeScript(createIfAbsentPath, ddl, prelude, "createIfAbsent");
    
    writeScriptReverse(dropIfExistsPath, ddl, prelude, "dropIfExists");
    
    System.out.println("SCHEMA_DUMP_WRITTEN_CREATE: " + createPath.toAbsolutePath());
    System.out.println("SCHEMA_DUMP_WRITTEN_CREATE_IF_ABSENT: " + createIfAbsentPath.toAbsolutePath());
    System.out.println("SCHEMA_DUMP_WRITTEN_DROP_IF_EXISTS: " + dropIfExistsPath.toAbsolutePath());
  }

  private static void writeScript(Path out, Queries ddl, String prelude, String mode) throws Exception {
    try (BufferedWriter w = Files.newBufferedWriter(out,
    	StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE)) {

      if (prelude != null && !prelude.isBlank()) {
        w.write(prelude);
        if (!prelude.endsWith("\n")) {
          w.write("\n");
        }
        w.write("\n");
      }

      for (Query query : ddl.queries()) {
        String sql = query.toString();
        
        if (sql == null) continue;
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) continue;
        
        if (CREATE_SCHEMA.matcher(trimmed).find()) {
        	continue;
        }
        
        if (trimmed.contains("schema")) {
        	System.out.println(trimmed + " ==============================did not catch");
        	continue;
        }
        if ("createIfAbsent".equals(mode)) {
            sql = SchemaToCreatelfAbsent.rewrite(sql);
        } else if ("dropIfExists".equals(mode)) {
        	sql = SchemaToDropIfExist.rewrite(sql);	
        }
        w.write(sql);
        w.write(";\n");
      }
    }
  }
  
  private static void writeScriptReverse(Path out, Queries ddl, String prelude, String mode) throws Exception {
	    try (BufferedWriter w = Files.newBufferedWriter(out,
	    	StandardCharsets.UTF_8,
	        StandardOpenOption.CREATE,
	        StandardOpenOption.TRUNCATE_EXISTING,
	        StandardOpenOption.WRITE)) {

	      if (prelude != null && !prelude.isBlank()) {
	        w.write(prelude);
	        if (!prelude.endsWith("\n")) {
	          w.write("\n");
	        }
	        w.write("\n");
	      }

	      List<String> qs = new ArrayList<String>(); 
	      for (Query query : ddl.queries()) {
	    	  String sql = query.toString();
	    	  if (sql == null) continue;
		      String trimmed = sql.trim();
		      if (trimmed.isEmpty()) continue;
		      qs.addFirst(trimmed);
	      }
	      
	      for (String sql : qs) {
	        
	        if (sql == null) continue;
	        String trimmed = sql.trim();
	        if (trimmed.isEmpty()) continue;
	        
	        if (CREATE_SCHEMA.matcher(trimmed).find()) {
	        	continue;
	        }
	        
	        if (trimmed.contains("schema")) {
	        	System.out.println(trimmed + " ==============================did not catch");
	        	continue;
	        }
	        if ("createIfAbsent".equals(mode)) {
	            sql = SchemaToCreatelfAbsent.rewrite(sql);
	        } else if ("dropIfExists".equals(mode)) {
	        	sql = SchemaToDropIfExist.rewrite(sql);	
	        }
	        w.write(sql);
	        w.write(";\n");
	      }
	    }
	  }

  private static String buildPrelude(String schema, String extensionsCsv) {
    StringBuilder sb = new StringBuilder();
    String effectiveSchema = (schema == null || schema.isBlank()) ? "public" : schema.trim();
    sb.append("create schema if not exists \"").append(effectiveSchema).append("\";\n");

    String extCsv = (extensionsCsv == null) ? "" : extensionsCsv.trim();
    if (!extCsv.isEmpty()) {
      for (String ext : extCsv.split(",")) {
        String e = ext == null ? "" : ext.trim();
        if (!e.isEmpty()) {
          sb.append("create extension if not exists ").append(e).append(";\n");
        }
      }
    }
    return sb.toString();
  }

  private static void backupIfExists(Path out) throws Exception {
    if (!Files.exists(out)) return;
    
    Date date = new Date() ;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
    String filename = dateFormat.format(date) + "-" + out.getFileName();
    Path dest = out.getParent().resolve(Path.of(filename));
    Files.move(out, dest);
  }

  private static Path withSuffix(Path out, String suffix) {
    String name = out.getFileName().toString();
    int dot = name.lastIndexOf('.');
    if (dot <= 0) {
      return out.getParent() != null ? out.getParent().resolve(name + suffix) : Path.of(name + suffix);
    }
    String stem = name.substring(0, dot);
    String ext = name.substring(dot); // includes '.'
    String newName = stem + suffix + ext;
    return out.getParent() != null ? out.getParent().resolve(newName) : Path.of(newName);
  }
}
