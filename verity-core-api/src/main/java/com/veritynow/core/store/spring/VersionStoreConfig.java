package com.veritynow.core.store.spring;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import com.veritynow.core.lock.LockingService;
import com.veritynow.core.lock.postgres.PgLockingService;
import com.veritynow.core.store.HashingService;
import com.veritynow.core.store.ImmutableBackingStore;
import com.veritynow.core.store.VersionStore;
import com.veritynow.core.store.base.DefaultHashingService;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.db.InodeManager;
import com.veritynow.core.store.db.JooqDirEntryRepository;
import com.veritynow.core.store.db.JooqInodePathSegmentRepository;
import com.veritynow.core.store.db.JooqInodeRepository;
import com.veritynow.core.store.db.JooqVersionMetaHeadRepository;
import com.veritynow.core.store.db.JooqVersionMetaRepository;
import com.veritynow.core.store.db.JooqVersionStore;
import com.veritynow.core.store.fs.ImmutableFSBackingStore;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.txn.PublishCoordinator;
import com.veritynow.core.txn.TransactionFinalizer;
import com.veritynow.core.txn.TransactionService;
import com.veritynow.core.txn.jooq.ContextAwareTransactionManager;
import com.veritynow.core.txn.jooq.JooqPublishCoordinator;
import com.veritynow.core.txn.jooq.JooqTransactionFinalizer;
import com.veritynow.core.txn.jooq.JooqTransactionService;

@Configuration
public class VersionStoreConfig {
	final static Logger  LOGGER = LogManager.getLogger(); 
	
	  @Bean
	public DSLContext dsl(DataSource ds) {
	   return DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
	}
	
	@Bean 
	HashingService hashingService(@Value("${verity.store.hash.algo:SHA-1}") String algo) throws NoSuchAlgorithmException {
		return new DefaultHashingService(algo);
	}
	
	@Bean 
	TransactionFinalizer transactionFinalizer(DSLContext dsl) {
		return new JooqTransactionFinalizer(dsl);
	}
	
	@Bean 
	PublishCoordinator publishCoordinator(DSLContext dsl, TransactionFinalizer finalizer)  {
		return new JooqPublishCoordinator(dsl, finalizer);
	}
	
	@Bean
	LockingService lockingService(
			DSLContext dsl,
			@Value("${verity.lock.ttl-ms:-1}") long lockTtlMs
	)  {
		return new PgLockingService(dsl, lockTtlMs);
	}
	
	
	@Bean 
	TransactionService transactionService(DSLContext dsl, PublishCoordinator coordinator)  {
		return new JooqTransactionService(dsl, coordinator);
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
    public JooqVersionMetaRepository jooqVersionMetaRepository(DSLContext dsl) {
    	return new JooqVersionMetaRepository(dsl);
    }
    
    @Bean 
    JooqInodePathSegmentRepository JooqInodePathSegmentRepository(DSLContext dsl) {
    	return new JooqInodePathSegmentRepository(dsl);
    }
    
    @Bean
    JooqDirEntryRepository jooqDirEntryRepository(DSLContext dsl) {
    	return new JooqDirEntryRepository(dsl);
    }
    
    @Bean
    JooqVersionMetaHeadRepository jooqVersionMetaHeadRepository(DSLContext dsl) {
    	return new JooqVersionMetaHeadRepository(dsl);
    }
    
    @Bean
    JooqInodeRepository jooqInodeRepository(DSLContext dsl) {
    	return new JooqInodeRepository(dsl);
    }
    
    @Bean
    public InodeManager inodeManger(JooqInodeRepository inodeRepo, JooqDirEntryRepository dirRepo, JooqInodePathSegmentRepository pathSegRepo, JooqVersionMetaHeadRepository headRepo, JooqVersionMetaRepository verRepo) {
    	return new InodeManager(inodeRepo, dirRepo, pathSegRepo, headRepo, verRepo);
    }
    
    @Bean
    @Primary
    public VersionStore<PK, BlobMeta, VersionMeta> versionJPAStore(
    		ImmutableBackingStore<String, BlobMeta> backingStore,
    		DSLContext dsl,
			InodeManager inodeManager,
            ContextAwareTransactionManager txnManager,
            LockingService lockingService
            
    ) {
		return new JooqVersionStore(backingStore, dsl, inodeManager, txnManager, lockingService);
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
