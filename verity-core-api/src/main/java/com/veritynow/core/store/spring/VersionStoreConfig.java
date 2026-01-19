package com.veritynow.core.store.spring;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.veritynow.core.lock.LockingService;
import com.veritynow.core.lock.postgres.PgLockingService;
import com.veritynow.core.store.HashingService;
import com.veritynow.core.store.ImmutableBackingStore;
import com.veritynow.core.store.VersionStore;
import com.veritynow.core.store.base.DefaultHashingService;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.fs.ImmutableFSBackingStore;
import com.veritynow.core.store.jpa.VersionJPAStore;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.txn.PublishCoordinator;
import com.veritynow.core.txn.TransactionFinalizer;
import com.veritynow.core.txn.TransactionService;
import com.veritynow.core.txn.jdbc.ContextAwareTransactionManager;
import com.veritynow.core.txn.jdbc.JdbcPublishCoordinator;
import com.veritynow.core.txn.jdbc.JdbcTransactionFinalizer;
import com.veritynow.core.txn.jdbc.JdbcTransactionService;

@Configuration
public class VersionStoreConfig {
	final static Logger  LOGGER = LogManager.getLogger(); 
	
	
	@Bean 
	HashingService hashingService(@Value("${verity.store.hash.algo:SHA-1}") String algo) throws NoSuchAlgorithmException {
		return new DefaultHashingService(algo);
	}

	@Bean 
	TransactionFinalizer transactionFinalizer() {
		return new JdbcTransactionFinalizer();
	}
	
	@Bean 
	PublishCoordinator publishCoordinator(JdbcTemplate jdbc, TransactionFinalizer finalizer)  {
		return new JdbcPublishCoordinator(jdbc, finalizer);
	}
	
	@Bean
	LockingService lockingService(JdbcTemplate jdbc)  {
		return new PgLockingService(jdbc);
	}
	
	
	@Bean 
	TransactionService transactionService(JdbcTemplate jdbc, PublishCoordinator coordinator)  {
		return new JdbcTransactionService(jdbc,  coordinator);
	}
	
	@Bean 
	ContextAwareTransactionManager contextAwareTransactionManager(TransactionService txnService)  {
		return new ContextAwareTransactionManager(txnService);
	}
	
	// Root directory for filesystem blobs, configurable via application.properties/yaml
    @Bean
    public ImmutableBackingStore<String, BlobMeta> immutableBackingStore(
            @Value("${verity.immutable.blobs.fs-root:./data}") String rootDir,
            HashingService hs
    ) {
        Path root = Path.of(rootDir).toAbsolutePath().normalize();
        return new ImmutableFSBackingStore(root, hs);
    }
    
//    @Bean
//    public VersionStore<PK, BlobMeta, VersionMeta> versionFSStore(
//            @Value("${verity.version.index.fs-root:./data}") String rootDir,
//            ImmutableBackingStore<String, BlobMeta> backingStore
//            
//    ) {
//        Path root = Path.of(rootDir).toAbsolutePath().normalize();
//        return new VersionFSStore(root, backingStore);
//    }
    
    @Bean
    @Primary
    public VersionStore<PK, BlobMeta, VersionMeta> versionJPAStore(
    		ImmutableBackingStore<String, BlobMeta> backingStore,
    		JdbcTemplate jdbc,
			com.veritynow.core.store.jpa.InodeManager inodeManager,
            ContextAwareTransactionManager txnManager,
            LockingService lockingService
            
    ) {
		return new VersionJPAStore(backingStore, jdbc, inodeManager, txnManager, lockingService);
    }
    
    @Bean
    SmartInitializingSingleton txnSanityCheck(ApplicationContext ctx) {
      return () -> {
    	 
        if (ctx.containsBeanDefinition("contextAwareTransactionManager")) {
        	LOGGER.info("\n\tTransaction support detected");
        }
        if (ctx.containsBeanDefinition("lockingService")) {
        	LOGGER.info("\n\tLocking support detected");
        }
      };
    }
}
