package com.veritynow.core.store.spring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Comparator;

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

import com.veritynow.core.store.tools.schema.SchemaManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import util.JSON;

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
		LOGGER.info("\n\tUsing Embedded Postgres as Datasource");
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
		LOGGER.debug("\n{}", JSON.MAPPER_PRETTY.writeValueAsString(config));
		config.setDataSource(ds);
		DataSource dataSource = new HikariDataSource(config);
		
		if (initEnabled && locations != null) {
			System.out.println("Executing scripts " + locations);
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
		killPostgres(PG_NAME);

		// if pg did not cleanup, just to make sure we don't delete anything aside from
		// "vn-pgdata-justtobesure"
		final Path dataDir = Path.of(TMP_DIR).resolve(PGDATA_DIR);

		if (Files.exists(dataDir) && Files.isDirectory(dataDir)) {
			deleteRecursively(dataDir);
		}

		// just create the directory, TEMP_DIR is expected to exists
		if (!Files.exists(dataDir))
			Files.createDirectory(dataDir);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			killPostgres(PG_NAME);
			deleteRecursively(dataDir);
		}));

		EmbeddedPostgres ep;

		ep = EmbeddedPostgres.builder().setDataDirectory(dataDir.toFile()).setCleanDataDirectory(true)
				.start();

		LOGGER.info("\n\tEmbedded Postgres Started");
		
		return ep;
	}
	
		
	private void initDB(DataSource ds) throws Exception {
		String[] locs = locations.split("\\s*,\\s*");
		
		
		try (Connection conn = ds.getConnection()) {

			DSLContext dsl = DSL.using(conn, SQLDialect.POSTGRES);
			for (String script : locs) {
				if (script != null && !(script = script.trim()).isBlank()) {
					LOGGER.info("Executing script : {}", script);
					
					if (!script.startsWith("/"))	script = "/" + script;
					
					try (InputStream is = getClass().getResourceAsStream(script)) {
						if (is == null) {
							continue;
						}
						SchemaManager.executeScript(dsl, is);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Acquisition of DB connection failed", e);
			throw e;
		}
	}

	private static void deleteRecursively(Path root) {
		LOGGER.info("\n\tDeleting dataDir {}", root);
		try {
			if (root == null || !Files.exists(root))
				return;
			Files.walk(root).sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}

	}

	private static void killPostgres(String processName) {

		try {
			ProcessHandle.allProcesses()
					.filter(ph -> ph.info().command()
							.map(cmd -> cmd.endsWith(processName) || cmd.endsWith(processName + ".exe")).orElse(false))
					.forEach(p -> {

						if (p != null) {
							try {
								ProcessHandle targetProcess = p;
								LOGGER.info("Found process: pid= {} - {}",
										 targetProcess.info().command().orElse("N/A"),  
										 targetProcess.pid() );
								
								targetProcess.destroy(); // Graceful termination

								LOGGER.info("Attempted to gracefully terminate process with pid = {}", targetProcess.pid());
								waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								if (targetProcess.isAlive()) {
									LOGGER.info("Graceful termination of process with pid = {} failed",	 targetProcess.pid() );
									targetProcess.destroyForcibly();
									LOGGER.info("Attempted to forcibly terminate process with pid = {}",
											 targetProcess.pid());
									waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								}
								if (targetProcess.isAlive()) {
									LOGGER.error("Unable to terminate process with pid = {}" , targetProcess.pid());
								} else {
									LOGGER.info("Process with pid = {} terminated",  + targetProcess.pid());
								}
							} catch (Throwable e) {
								e.printStackTrace();
							}
						} else {
							LOGGER.info("Process '{}' not found.",  processName);
						}
					});
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void waitForTemination(ProcessHandle targetProcess, int maxInterval,
			Duration waitDurationPerInterval) {
		int wait = 0;
		while (targetProcess.isAlive() && wait < maxInterval) {
			sleep(waitDurationPerInterval);
			wait++;
		}
	}

	private static void sleep(Duration d) {
		try {
			Thread.sleep(d);
		} catch (Throwable e) {
			// ignore
		}
	}

}
