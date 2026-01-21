package com.veritynow.core.store.tools.pg;

import java.lang.System.Logger;
import java.nio.file.Path;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.ForcedType;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.veritynow.core.store.spring.EmbeddedPostgresConfig;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

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

  /**
   * Treat Postgres 'ltree' columns as VARCHAR in generated sources (pragmatic default).
   */
  
/**
 * Enable first-class ltree binding (recommended for path-centric design).
 * When enabled, generated columns of type ltree become Field<LTree>.
 */
@Value("${verity.jooq.codegen.ltreeBindingEnabled:true}")
private boolean ltreeBindingEnabled;

@Value("${verity.jooq.codegen.ltreeUserType:com.veritynow.core.store.jooq.LTree}")
private String ltreeUserType;

@Value("${verity.jooq.codegen.ltreeBinding:com.veritynow.core.store.jooq.LTreeBinding}")
private String ltreeBinding;

@Value("${verity.jooq.codegen.ltreeAsVarchar:true}")
  private boolean ltreeAsVarchar;

  /**
   * Regex to match ltree columns; default targets your standard naming (scope_key).
   */
  @Value("${verity.jooq.codegen.ltreeColumnRegex:.*\\.scope_key}")
  private String ltreeColumnRegex;

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
  
      Database database = new Database()
          .withName("org.jooq.meta.postgres.PostgresDatabase")
          .withInputSchema(schema)
          .withIncludes(includes)
          .withExcludes(excludes);

      if (ltreeBindingEnabled) {
  database.withForcedTypes(
      new ForcedType()
          .withUserType(ltreeUserType)
          .withBinding(ltreeBinding)
          .withIncludeTypes("ltree")
          .withIncludeExpression(ltreeColumnRegex)
  );
} else if (ltreeAsVarchar) {
  database.withForcedTypes(
      new ForcedType()
          .withName("VARCHAR")
          .withIncludeTypes("ltree")
          .withIncludeExpression(ltreeColumnRegex)
  );
}


      Generator generator = new Generator()
          .withDatabase(database)
          .withTarget(
              new Target()
                  .withPackageName(packageName)
                  .withDirectory(outputDir)
          );

      EmbeddedPostgres ep = EmbeddedPostgresConfig.getEmbedded();

      String url = ep.getJdbcUrl("postgres", "");
      
      System.out.println("JOOQ_CODEGEN_USING_JDBC URL: " + url);
      
      Configuration cfg = new Configuration()
          .withJdbc(new Jdbc().withUrl(url)
          .withDriver("org.postgresql.Driver"))
          .withGenerator(generator);

      GenerationTool.generate(cfg);

      System.out.println("JOOQ_CODEGEN_WRITTEN: " + Path.of(outputDir).toAbsolutePath());
    }
 }

