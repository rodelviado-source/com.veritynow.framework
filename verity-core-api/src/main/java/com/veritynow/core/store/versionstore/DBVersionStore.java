package com.veritynow.core.store.versionstore;

import static com.veritynow.core.store.txn.TransactionResult.AUTO_COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;

import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.ImmutableStore;
import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.AbstractStore;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.base.StoreContext;
import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.txn.jooq.ContextAwareTransactionManager;
import com.veritynow.core.store.versionstore.repo.RepositoryManager;


public class DBVersionStore extends AbstractStore<PK, BlobMeta, VersionMeta> implements  TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle>  {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final ImmutableStore<String, BlobMeta, BlobMeta> backingStore;
    private final ContextAwareTransactionManager txnManager;
    private final LockingService lockingService;

    private final int maxAttempts=5;
    private final int delayBetweenAttempts=100;
     
    
	private final RepositoryManager repositoryManager;

    public DBVersionStore(
            ImmutableStore<String, BlobMeta, BlobMeta> backingStore,
			DSLContext dsl,
			RepositoryManager repositoryManager,
            ContextAwareTransactionManager txnManager,
            LockingService lockingService
    ) {
   
    	super(Objects.requireNonNull(backingStore, "backingstore required").getHashingService());
        this.backingStore = backingStore;
        this.txnManager = txnManager;
		this.lockingService = lockingService;
		this.repositoryManager = Objects.requireNonNull(repositoryManager, "repositoryManager required");
		
    	repositoryManager.ensureRootInode();
        
        LOGGER.info(
        		"Inode-backed Versioning Store started. Using {} for Immutable Storage", backingStore.getClass().getName()
      	);
    }
    
 
    //convenience if transaction support is avialable
    @Override
	public String begin() {
    	if (txnManager != null)
    		return txnManager.begin();
    	return null;
	}

    @Override
	public void commit() {
    	if (txnManager != null)
    		txnManager.commit();
	}

	@Override
	public void rollback() {
		if (txnManager != null)
			txnManager.rollback();
	}

	@Override
	public CloseableLockHandle acquire(List<String> paths) {
		if (lockingService != null) {
			LockHandle lh = lockingService.acquire(paths);
			CloseableLockHandle clh = new CloseableLockHandle(lockingService, lh); 
			return clh;
		}	
		return null;
	}

	


	@Override
	public CloseableLockHandle acquireLock(String... paths) {
		return acquire(List.of(paths));
	}


	@Override
	public List<Long> findActiveAdvisoryLocks(String txnId) {
		if (lockingService != null) {
			return lockingService.findActiveAdvisoryLocks(txnId);
		}
		return List.of();
	}

	@Override
	public Optional<Long> findActiveAdvisoryLock(String txnId, String path) {
		if (lockingService != null) {
			return lockingService.findActiveAdvisoryLock(txnId, path);
		}
		return Optional.empty();
	}

	@Override
	public CloseableLockHandle tryAcquireLock(List<String> paths, int maxAttempts, int delayBetweenAttemptsMs) {
		try {
			if (lockingService != null) {
					LockHandle lh = lockingService.tryAcquireLock(paths, maxAttempts, delayBetweenAttemptsMs);
					CloseableLockHandle clh = new CloseableLockHandle(lockingService, lh); 
					return clh;
			}
		} catch (Exception e) {
			LOGGER.warn("Unable to acquire lock for path {}", paths);
			throw new IllegalStateException("Unable to acquire lock", e);
		}
		return null;
	}
	
	@Override
	public CloseableLockHandle tryAcquireLock(List<String> paths) {
		try {
			if (lockingService != null) {
					LockHandle lh = lockingService.tryAcquireLock(paths, maxAttempts, delayBetweenAttempts);
					CloseableLockHandle clh = new CloseableLockHandle(lockingService, lh); 
					return clh;
			}  
		}catch (Exception e) {
			LOGGER.warn("Unable to acquire lock for path {}", paths);
			throw new IllegalStateException("Unable to acquire lock", e);
		}
		return null;
	}
	
	@Override
	public void release(CloseableLockHandle handle) {
		try (handle) {
		} catch (Exception ignore) {
			//throw new RuntimeException(e);
		}
	}
	
	@Override
    public Optional<InputStream> getContent(PK key) throws IOException {
    	Objects.requireNonNull(key, "key");
    	Objects.requireNonNull(key.hash(), "hash");
    	try {
    		return backingStore.retrieve(key.hash());
        } catch (IOException e) {
        	 LOGGER.error("Unable to retrieve content meta hash={}", key.hash(), e);
        	 throw new IOException("Unable to retrieve content hash={}", e);
        }
    }
	
	@Override
	public Optional<BlobMeta> getContentMeta(PK key) throws IOException {
		Objects.requireNonNull(key, "key");
    	Objects.requireNonNull(key.hash(), "hash");
    	try {
    		return backingStore.getMeta(key.hash());
        } catch (IOException e) {
        	 LOGGER.error("Unable to retrieve content meta hash={}", key.hash(), e);
        	 throw new IOException("Unable to retrieve content meta hash={}", e);
        }
	}

    @Override
    public boolean exists(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return getLatestVersion(key).isPresent();
    }

    
    @Override
    public boolean pathExists(PK  key) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "path");
        return repositoryManager.pathExists(key.path());
    }
    
    // ----------------------------
 	// CREATE - create a new version with new UUID(storeId) , under path/{storeId}
 	// 
    @Override
    public Optional<VersionMeta> create(PK key, BlobMeta meta, InputStream content) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "path");
        Objects.requireNonNull(content, "content");
        return createNewVersion(key, meta, content, StoreOperation.Created);
    }

    // ----------------------------
 	// CREATE - create a new version with the supplied id, under path/{id}
 	//          if id is null will generate a new UUID{storeId} and create a new version under path/{storeId}
 	//
    @Override
	public Optional<VersionMeta> create(PK key, BlobMeta meta, InputStream content, String id) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "path");
        Objects.requireNonNull(content, "content");
        return createNewVersion(key, meta, content, StoreOperation.Created, id);
	}

	@Override
    public Optional<VersionMeta> update(PK key, InputStream content) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(content, "content");

        String path = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(PK.path(path));
        if (opt.isEmpty()) {
        	LOGGER.warn("Cannot update a non-exisitent path {}", path);
        	return Optional.empty();
        }
        VersionMeta mx = opt.get();
        if (isDeleted(mx)) {
            LOGGER.warn("Cannot update a deleted path {}" , path);
            return Optional.empty();
        }
        return appendVersion(content, StoreOperation.Updated, mx, key);
    }

    @Override
    public Optional<InputStream> read(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String path = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(PK.path(path));

        if (opt.isPresent()) {
        	VersionMeta mx = opt.get();
            if (!isDeleted(mx)) {
                return backingStore.retrieve(mx.hash());
            }
            LOGGER.info("Attempt to read a deleted blob at path {}", path);
        }
        
        LOGGER.warn("No version of blob exists at path {}", path);
        return Optional.empty();
    }

    @Override
    public Optional<VersionMeta> delete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String path = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(PK.path(path));

        if (opt.isEmpty()) {
            LOGGER.warn("Nothing to delete at path {}" , path);
            return Optional.empty();
        }
        VersionMeta mx = opt.get();
        if (isDeleted(mx)) {
        	LOGGER.info("Attempt to delete an already deleted  path {}" , path);
            return Optional.empty();
        }
        return appendVersion(null, StoreOperation.Deleted, mx, key);
    }

    @Override
    public Optional<VersionMeta> undelete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String path = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(PK.path(path));

        if (opt.isEmpty()) {
            LOGGER.warn("Unable to undelete {}" , path);
            return Optional.empty();
        }
        VersionMeta mx = opt.get();

        if (!isDeleted(mx)) {
            LOGGER.warn("Can only undelete a deleted path " + path);
            return Optional.empty();
        }
        return appendVersion(null, StoreOperation.Undeleted, mx, key);
    }

    
    @Override
    public Optional<VersionMeta> restore(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(key.hash(), "key.hash");

        String path = PathUtils.normalizePath(key.path());
        String hash = key.hash();
        
        List<VersionMeta> l = getAllVersions(PK.path(path));
        
        Optional<VersionMeta> opt = l.stream().filter((m) -> hash.equals(m.hash())).findFirst();

        if (opt.isEmpty()) {
            LOGGER.warn("Unable to restore path {} hash {}", path, hash);
            return Optional.empty();
        }
        
        VersionMeta mx = opt.get();

        
        return appendVersion(null, StoreOperation.Restored, mx, key);
    }
    
    
    @Override
    public Optional<VersionMeta> getLatestVersion(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return repositoryManager.getLatestVersion(key.path());
    }

    @Override
    public List<VersionMeta> getChildrenLatestVersion(PK key) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return repositoryManager.getChildrenLatestVersion(key.path());
     }

    @Override
    public List<String> getChildrenPath(PK key) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return repositoryManager.getChildrenPath(key.path());
    }

    @Override
    public List<VersionMeta> getAllVersions(PK key) throws IOException {
    	return repositoryManager.getAllVersions(key.path());
    }
   
    private Optional<VersionMeta> createNewVersion(
            PK key,
            BlobMeta meta,
            InputStream content,
            StoreOperation operation
            
    ) throws IOException {
    	return createNewVersion(key, meta, content, operation, null);
    }
    
    
    private Optional<VersionMeta> createNewVersion(
            PK key,
            BlobMeta meta,
            InputStream content,
            StoreOperation operation,
            String forcedId
    ) throws IOException {

        final String id =
        (forcedId != null && !forcedId.isBlank()) ? forcedId : UUID.randomUUID().toString();
        
        String path = PathUtils.normalizePath(key.path());
        
        path = path + "/" + id;
        
        // Save payload in immutable store, store is authority for attr/hash (same as FS) :contentReference[oaicite:13]{index=13}
        BlobMeta blobMeta = backingStore.save(meta, content).orElseThrow();

        return Optional.of(persistAndPublish(path, blobMeta, operation));
        
        
    }

    private Optional<VersionMeta> appendVersion(
            InputStream content,
            StoreOperation operation,
            VersionMeta current,
            PK pkey
            
    ) throws IOException {

    	Objects.requireNonNull(current);
    	Objects.requireNonNull(pkey);
    	Objects.requireNonNull(operation);
        String path = PathUtils.normalizePath(pkey.path());
        BlobMeta blobMeta = current.blobMeta();

        switch (operation) {
            case Updated:
                blobMeta = backingStore.save(current.name(), current.mimeType() , content).orElseThrow();
                break;
            case Deleted:
            case Undeleted:
            case Restored:
                // no payload change (keep hash/attr)
                break;
            default:
                throw new IOException("Invalid Store Operation " + operation.name());
        }

        return Optional.of(persistAndPublish(path, blobMeta, operation));
        
    }

    
    
    private VersionMeta persistAndPublish(String path, BlobMeta blobMeta,  StoreOperation operation) throws IOException {
    	//Capture global context/transaction context, if no active context then create a store context 
    	//with sane defaults
    	
    	//If no active gobal/transaction context StoreContext is just "operation" capture 
    	//read/write/update/restore etc with synthesize ids 
    	//transactionId is set to null and transactionResult is set to AUTO_COMMITTED--
    	StoreContext sc = StoreContext.create(operation.name());
        PathEvent pe = new PathEvent(path,  sc);
        VersionMeta vm = new VersionMeta(blobMeta, pe);
        
        // repo is the authoritative write: insert the version row and move HEAD in one statement.
        // This keeps inode/version ids entirely within the persistence layer.
//        try (@SuppressWarnings("unused") ContextScope scope = Context.ensureContext(sc.operation() + "-" +sc.transactionResult());
//        		@SuppressWarnings("unused")	CloseableLockHandle lock = tryAcquireLock(path)
//        				) {
	        if (AUTO_COMMITTED.equals(sc.transactionResult())) {
	        		return repositoryManager.persistAndPublish(vm); 
	        } else if (IN_FLIGHT.equals(sc.transactionResult())) {
	        	// Under an explicit store transaction, this write must NOT be committed yet.
	        	// There is exactly one DB commit, and it is only legal at transaction finalization.
	        	//
	        	// - commit() transitions IN_FLIGHT → COMMITTED, then moves HEAD
	        	// - rollback() transitions IN_FLIGHT → ROLLED_BACK, no HEAD movement
	        	//
	        	// If this IN_FLIGHT write is ever made durable before explicit finalization,
	        	// that indicates an illegal early DB commit and is a bug / red flag.
        		return repositoryManager.persist(vm);
	        } 
	        else throw new IllegalStateException("Expecting " + IN_FLIGHT + "  got " + sc.transactionResult() + " instead");
//        } catch (Exception e) {
//        	throw new IllegalStateException("Failed to acquire lock ",e);
//        }
    }
 
    
    private static boolean isDeleted(VersionMeta m ) {
    	return StoreOperation.Deleted().equals(m.operation());
    }


	


	   
}
