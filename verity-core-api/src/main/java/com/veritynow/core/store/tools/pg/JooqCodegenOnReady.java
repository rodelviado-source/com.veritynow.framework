package com.veritynow.core.store.tools.pg;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;



/**
 * Dev-only: generate jOOQ sources from the live Postgres catalog (embedded or remote) after the
 * application has started and the schema has been created (Hibernate create/update OR Liquibase).
 *
 * Enabled only when verity.jooq.codegen.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "verity.jooq.codegen.enabled", havingValue = "true")
public class JooqCodegenOnReady {
   
  @Autowired
  private DataSource dataSource;

@Value("${verity.jooq.codegen.schema:public}")
  private String schema;

  @Value("${verity.jooq.codegen.includes:vn_.*}")
  private String includes;

  @Value("${verity.jooq.codegen.excludes:databasechangelog.*|databasechangeloglock.*}")
  private String excludes;

  @Value("${verity.jooq.codegen.package:dev.generated.jooq}")
  private String packageName;

  @Value("${verity.jooq.codegen.output:target/generated-sources/jooq}")
  private String outputDir;

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
  
      Database database = new Database()
          .withName("org.jooq.meta.postgres.PostgresDatabase")
          .withInputSchema(schema)
          .withIncludes(includes)
          .withExcludes(excludes);


      Generator generator = new Generator()
          .withDatabase(database)
          .withTarget(
              new Target()
                  .withPackageName(packageName)
                  .withDirectory(outputDir)
          );
      // Use the live DataSource provided by Spring (embedded-postgres or external)
      String url;
      String username;
      try (var conn = dataSource.getConnection()) {
        url = conn.getMetaData().getURL();
        username = conn.getMetaData().getUserName();
        
        
      }
      System.out.println("JOOQ_CODEGEN_USING_JDBC URL: " + url);
      
      Configuration cfg = new Configuration()
          .withJdbc(new Jdbc().withUrl(url).withUser(username)
          .withDriver("org.postgresql.Driver"))
          .withGenerator(generator);

      GenerationTool.generate(cfg);

      System.out.println("JOOQ_CODEGEN_WRITTEN: " + Path.of(outputDir).toAbsolutePath());
    }
 }
