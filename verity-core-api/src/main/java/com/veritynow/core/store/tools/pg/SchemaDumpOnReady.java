package com.veritynow.core.store.tools.pg;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "verity.schema.dump.enabled", havingValue = "true")
public class SchemaDumpOnReady {

  private final DataSource dataSource;

  @Value("${verity.schema.dump.schema:public}")
  private String schema;

  @Value("${verity.schema.dump.path:ddl/schema-baseline.sql}")
  private String outPath;

  public SchemaDumpOnReady(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
    PgSchemaDumper.dumpSchema(dataSource, schema, Path.of(outPath));
    System.out.println("SCHEMA_DUMP_WRITTEN: " + Path.of(outPath).toAbsolutePath());
  }
}
