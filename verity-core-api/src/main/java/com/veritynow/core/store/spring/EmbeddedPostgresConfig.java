package com.veritynow.core.store.spring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;


@Configuration
@Profile("embedded-postgres")
public class EmbeddedPostgresConfig {
	final static Logger  LOGGER = LogManager.getLogger();
	
	
	@Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        // H2-like behavior: fresh cluster per app run
        Path dataDir = Files.createTempDirectory("vn-pgdata-");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(dataDir)));
        LOGGER.info("\n\tEmbedded Postgres Started");
        return EmbeddedPostgres.builder()
                .setDataDirectory(dataDir.toFile()).setPort(5432)
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres pg) throws IOException {
        // Provides a ready JDBC DataSource to the embedded instance
    	LOGGER.info("\n\tUsing Postgres as Datasource");
    	LOGGER.info("\n\tAdding extension");
        DataSource ds = pg.getPostgresDatabase();
        return ds;
    }

    private static void deleteRecursively(Path root) {
    	LOGGER.info("\n\tDeleting dataDir {}", root);
        try {
            if (root == null || !Files.exists(root)) return;
            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
    
}
