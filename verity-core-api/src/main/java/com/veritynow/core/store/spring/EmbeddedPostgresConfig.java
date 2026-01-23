package com.veritynow.core.store.spring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
	final static String PG_NAME = "postgres";
	final static String TMP_DIR = System.getProperty("java.io.tmpdir");
	final static String PGDATA_DIR = "vn-pgdata-justtobesure";
	final static Logger  LOGGER = LogManager.getLogger();
	static EmbeddedPostgres ep;
	
	@Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        // H2-like behavior: fresh cluster per app run
		
		//terminate if there is a stale postgres running to release locks on the tempDir
		//OS specific for now but will make all of this OS aware
		killPostgres(PG_NAME);
		
		//if pg did not cleanup, just to make sure we don't delete anything aside from "vn-pgdata-justtobesure"
		final Path dataDir = Path.of(TMP_DIR).resolve(PGDATA_DIR);
		
		if (Files.exists(dataDir) &&  Files.isDirectory(dataDir)) {
			deleteRecursively(dataDir);
		} 
		
		Files.createDirectory(dataDir);
        
		Runtime.getRuntime().addShutdownHook(new Thread(() -> { killPostgres(PG_NAME); deleteRecursively(dataDir); }));
		
        ep = EmbeddedPostgres.builder()
                .setDataDirectory(dataDir.toFile())
                .setPort(5432)
                .setCleanDataDirectory(true)
                .start();
        
        LOGGER.info("\n\tEmbedded Postgres Started");
        return ep;
    }
	
    @Bean
    public DataSource dataSource(EmbeddedPostgres pg) throws IOException {
        // Provides a ready JDBC DataSource to the embedded instance
    	LOGGER.info("\n\tUsing Postgres as Datasource");
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
    
    
    private static void killPostgres(String processName) {
    	
    	try {
    	 ProcessHandle.allProcesses()
    	    .filter(ph -> ph.info().command().map(cmd -> cmd.endsWith(processName) || cmd.endsWith(processName+ ".exe") ).orElse(false))
    	    .forEach(p -> {

    	if (p != null) {
    		try {
    	    ProcessHandle targetProcess = p;
    	    System.out.println("Found process: pid= " + targetProcess.pid() + " - " + targetProcess.info().command().orElse("N/A"));
    	    targetProcess.destroy(); // Graceful termination
    	    
    	    System.out.println("Attempted to gracefully terminate process with pid = " + targetProcess.pid());
    	    waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
    	    if (targetProcess.isAlive()) {
    	    	System.out.println("Graceful termination of process with pid = " + targetProcess.pid() + " failed");
    	    	targetProcess.destroyForcibly();
    	    	System.out.println("Attempted to forcibly terminate process with pid = " + targetProcess.pid());
    	    	waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
    	    }
    	    if (targetProcess.isAlive()) {
	    		System.err.println("Unable to terminate process with pid = " + targetProcess.pid());
	    	} else {
	    		System.out.println("Process with pid = " + targetProcess.pid() + " terminated");
	    	}
    		} catch (Throwable e) {
    			e.printStackTrace();
    		}
    	} else {
    	    System.out.println("Process '" + processName + "' not found.");
    	}
    	    });
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}
    }
    
    private static void waitForTemination(ProcessHandle targetProcess, int maxInterval,  Duration waitDurationPerInterval) {
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
	    	//ignore
	    }
    }
}
