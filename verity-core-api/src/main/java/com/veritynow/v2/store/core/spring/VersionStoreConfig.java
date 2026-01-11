package com.veritynow.v2.store.core.spring;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.veritynow.v2.store.HashingService;
import com.veritynow.v2.store.ImmutableBackingStore;
import com.veritynow.v2.store.VersionStore;
import com.veritynow.v2.store.core.DefaultHashingService;
import com.veritynow.v2.store.core.PK;
import com.veritynow.v2.store.core.fs.ImmutableFSBackingStore;
import com.veritynow.v2.store.core.fs.VersionFSStore;
import com.veritynow.v2.store.core.jpa.DirEntryRepository;
import com.veritynow.v2.store.core.jpa.InodeRepository;
import com.veritynow.v2.store.core.jpa.VersionJPAStore;
import com.veritynow.v2.store.core.jpa.VersionMetaHeadRepository;
import com.veritynow.v2.store.core.jpa.VersionMetaRepository;
import com.veritynow.v2.store.meta.BlobMeta;
import com.veritynow.v2.store.meta.VersionMeta;

@Configuration
public class VersionStoreConfig {
	
	
	@Bean 
	HashingService hashingService(@Value("${verity.store.hash.algo:SHA-1}") String algo) throws NoSuchAlgorithmException {
		return new DefaultHashingService(algo);
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
    public VersionStore<PK, BlobMeta, VersionMeta> versionFSStore(
            @Value("${verity.version.index.fs-root:./data}") String rootDir,
            ImmutableBackingStore<String, BlobMeta> backingStore
            
    ) {
        Path root = Path.of(rootDir).toAbsolutePath().normalize();
        return new VersionFSStore(root, backingStore);
    }
    
    @Bean
    @Primary
    public VersionStore<PK, BlobMeta, VersionMeta> versionJPAStore(
    		ImmutableBackingStore<String, BlobMeta> backingStore,
            InodeRepository inodeRepo,
            DirEntryRepository dirRepo,
            VersionMetaRepository verRepo,
            VersionMetaHeadRepository headRepo
            
    ) {
        return new VersionJPAStore(backingStore,
                inodeRepo,
                dirRepo,
                verRepo,
                headRepo);
    }
}
