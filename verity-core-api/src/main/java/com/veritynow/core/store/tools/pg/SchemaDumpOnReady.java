package com.veritynow.core.store.tools.pg;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Queries;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import static com.veritynow.core.store.persistence.jooq.Public.PUBLIC; 



@Component
@ConditionalOnProperty(name = "verity.schema.dump.enabled", havingValue = "true")
public class SchemaDumpOnReady {

  private final DSLContext dsl;

  @Value("${verity.schema.dump.schema:public}")
  private String schema;

  @Value("${verity.schema.dump.path:ddl/schema-baseline.sql}")
  private String outPath;

  public SchemaDumpOnReady(DSLContext dsl) {
    this.dsl = dsl;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
    
	  Queries ddl = dsl.ddl(PUBLIC);  
	  
	  Path out = Path.of(outPath);
	  
	  Files.createDirectories(out.toAbsolutePath().getParent());
	  
	  try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
	          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
		  for (Query query : ddl.queries()) {
		        w.write(query.toString() + ";\n"); // Prints the DDL SQL statement
		   }
		  
	  }

	  
	  
    System.out.println("SCHEMA_DUMP_WRITTEN: " + Path.of(outPath).toAbsolutePath());
  }
}
