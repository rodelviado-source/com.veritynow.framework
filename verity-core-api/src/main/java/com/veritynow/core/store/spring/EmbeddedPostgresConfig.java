package com.veritynow.core.store.spring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.veritynow.core.store.tools.schema.SchemaManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import util.FSUtil;
import util.JSON;
import util.ProcessUtil;

@Component
@Profile("embedded-postgres")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmbeddedPostgresConfig {
	final static String PG_NAME = "postgres";
	final static String TMP_DIR = System.getProperty("java.io.tmpdir");
	final static String PGDATA_DIR = "vn-pgdata-justtobesure";
	final static Logger LOGGER = LogManager.getLogger();
	

	@Value("${verity.init.db.enabled:true}")
	private Boolean initEnabled;

	@Value("${verity.init.db.schema.locations:schema/schema-create.sql}")
	private String locations;

	@Bean
	//let spring configure as defined in properties
	@ConfigurationProperties(prefix = "spring.datasource.hikari") 
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}

	@Bean
	public DataSource dataSource(EmbeddedPostgres pg, HikariConfig config) throws IOException {
		// Provides a ready JDBC DataSource to the embedded instance
		
		config.setPoolName("embeddedPostgresHikariCP");
		String url = null;
		String username = null;

		//Configure only the Datasource properties 
		//Leave ConnectionPool properties as is, it is configured already by spring
		DataSource ds = pg.getPostgresDatabase();
		try (Connection conn = ds.getConnection()) {
			DatabaseMetaData m = conn.getMetaData();
			url = m.getURL();
			username = m.getUserName();
		} catch (Exception e) {

		}
		if (url != null)
			config.setJdbcUrl(url);
		if (username != null)
			config.setUsername(username);

		JSON.MAPPER_PRETTY.setDefaultPropertyInclusion(Include.NON_NULL);
		JSON.MAPPER_PRETTY.setDefaultPropertyInclusion(Include.NON_EMPTY);
		LOGGER.info("\nUsing Embedded Postgress and Hikari Connection Pool  {}", JSON.MAPPER_PRETTY.writeValueAsString(config));
		config.setDataSource(ds);
		DataSource dataSource = new HikariDataSource(config);
		
		if (initEnabled && locations != null) {
			try {
				initDB(dataSource);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return dataSource;
		
	}

	@Bean
	public EmbeddedPostgres embeddedPostgres() throws Exception {
		// H2-like behavior: fresh cluster per app run

		// terminate if there is a stale postgres running to release locks on the
		// tempDir
		// OS specific for now but will make all of this OS aware
		ProcessUtil.killProcess(PG_NAME);

		// if pg did not cleanup, just make sure we don't delete anything aside from
		// "vn-pgdata-justtobesure"
		final Path dataDir = Path.of(TMP_DIR).resolve(PGDATA_DIR);

		if (Files.exists(dataDir) && Files.isDirectory(dataDir)) {
			FSUtil.deleteRecursively(dataDir);
		}

		// just create the directory, TEMP_DIR is expected to exists
		if (!Files.exists(dataDir))
			Files.createDirectory(dataDir);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ProcessUtil.killProcess(PG_NAME);
			FSUtil.deleteRecursively(dataDir);
		}));

		EmbeddedPostgres ep;

		ep = EmbeddedPostgres.builder().
				setDataDirectory(dataDir.toFile())
				.setPort(5432)
				.setCleanDataDirectory(true)
				.start();

		LOGGER.info("Embedded Postgres Started");
		
		return ep;
	}
	
		
	private void initDB(DataSource ds) throws Exception {
		if (locations == null && locations.trim().isBlank()) return;
		
		String[] locs = locations.split("\\s*,\\s*");
		
		
		try (Connection conn = ds.getConnection()) {

			DSLContext dsl = DSL.using(conn, SQLDialect.POSTGRES);
			for (String script : locs) {
				if (script != null && !(script = script.trim()).isBlank()) {
					
					
					if (!script.startsWith("/"))	script = "/" + script;
					
					try (InputStream is = getClass().getResourceAsStream(script)) {
						if (is == null) {
							continue;
						}
						if (SchemaManager.executeScript(dsl, is)) {
							LOGGER.info("{} executed successfully", script);
						} else {
							LOGGER.info("{} execution failed", script);
						}
					} catch (Exception e) {
						LOGGER.info("{} execution failed", script);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Acquisition of DB connection failed", e);
			throw e;
		}
	}

	

	
}
