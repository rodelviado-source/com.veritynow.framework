package com.veritynow.v2.store.core.spring;

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

import com.veritynow.v2.lock.LockingService;
import com.veritynow.v2.lock.jpa.PgLockingService;
import com.veritynow.v2.store.HashingService;
import com.veritynow.v2.store.ImmutableBackingStore;
import com.veritynow.v2.store.VersionStore;
import com.veritynow.v2.store.core.DefaultHashingService;
import com.veritynow.v2.store.core.PK;
import com.veritynow.v2.store.core.StoreResult;
import com.veritynow.v2.store.core.fs.ImmutableFSBackingStore;
import com.veritynow.v2.store.core.fs.VersionFSStore;
import com.veritynow.v2.store.core.jpa.DirEntryRepository;
import com.veritynow.v2.store.core.jpa.InodePathSegmentRepository;
import com.veritynow.v2.store.core.jpa.InodeRepository;
import com.veritynow.v2.store.core.jpa.VersionJPAStore;
import com.veritynow.v2.store.core.jpa.VersionMetaHeadRepository;
import com.veritynow.v2.store.core.jpa.VersionMetaRepository;
import com.veritynow.v2.store.meta.BlobMeta;
import com.veritynow.v2.store.meta.VersionMeta;
import com.veritynow.v2.txn.PublishCoordinator;
import com.veritynow.v2.txn.TransactionService;
import com.veritynow.v2.txn.impl.ContextAwareTransactionManager;
import com.veritynow.v2.txn.impl.JdbcPublishCoordinator;
import com.veritynow.v2.txn.impl.JdbcTransactionService;
import com.veritynow.v2.txn.impl.JpaVersionStoreTxnFinalizer;
import com.veritynow.v2.txn.spi.StoreTxnFinalizer;

@Configuration
public class VersionStoreConfig {
	final static Logger  LOGGER = LogManager.getLogger(); 
	
	
	@Bean 
	HashingService hashingService(@Value("${verity.store.hash.algo:SHA-1}") String algo) throws NoSuchAlgorithmException {
		return new DefaultHashingService(algo);
	}

	@Bean 
	StoreTxnFinalizer storeTxnFinalizer() {
		return new JpaVersionStoreTxnFinalizer();
	}
	
	@Bean 
	PublishCoordinator publishCoordinator(JdbcTemplate jdbc, StoreTxnFinalizer store)  {
		return new JdbcPublishCoordinator(jdbc, store);
	}
	
	@Bean
	LockingService lockingService(JdbcTemplate jdbc
            )  {
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
    
    @Bean
    public VersionStore<PK, BlobMeta, VersionMeta, StoreResult> versionFSStore(
            @Value("${verity.version.index.fs-root:./data}") String rootDir,
            ImmutableBackingStore<String, BlobMeta> backingStore
            
    ) {
        Path root = Path.of(rootDir).toAbsolutePath().normalize();
        return new VersionFSStore(root, backingStore);
    }
    
    @Bean
    @Primary
    public VersionStore<PK, BlobMeta, VersionMeta, StoreResult> versionJPAStore(
    		ImmutableBackingStore<String, BlobMeta> backingStore,
    		JdbcTemplate jdbc,
            InodeRepository inodeRepo,
            DirEntryRepository dirRepo,
            InodePathSegmentRepository pathSegRepo,
            VersionMetaRepository verRepo,
            VersionMetaHeadRepository headRepo,
            ContextAwareTransactionManager txnManager,
            LockingService lockingService
            
    ) {
        return new VersionJPAStore(backingStore, jdbc,
                inodeRepo,
                dirRepo,
                pathSegRepo,
                verRepo,
                headRepo, txnManager, lockingService);
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
