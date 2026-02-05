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
import com.veritynow.core.store.ImmutableBackingStore;
import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.AbstractStore;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.base.PathEvent;
import com.veritynow.core.store.base.StoreContext;
import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.txn.jooq.ContextAwareTransactionManager;
import com.veritynow.core.store.versionstore.repo.RepositoryManager;


public class DBVersionStore extends AbstractStore<PK, BlobMeta> implements  TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle>  {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final ImmutableBackingStore<String, BlobMeta> backingStore;
    private final ContextAwareTransactionManager txnManager;
    private final LockingService lockingService;
    
    
	private final RepositoryManager repositoryManager;

    public DBVersionStore(
            ImmutableBackingStore<String, BlobMeta> backingStore,
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
	public Optional<List<Long>> findActiveAdvisoryLocks(String txnId) {
		if (lockingService != null) {
			return lockingService.findActiveAdvisoryLocks(txnId);
		}
		return Optional.empty();
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
		if (lockingService != null) {
				LockHandle lh = lockingService.tryAcquireLock(paths, maxAttempts, delayBetweenAttemptsMs);
				CloseableLockHandle clh = new CloseableLockHandle(lockingService, lh); 
				return clh;
		}
			 
		return null;
	}

	@Override
	public CloseableLockHandle tryAcquireLock(String path) {
    	if (lockingService != null)
    		return tryAcquireLock(List.of(path),5, 100);
    	LOGGER.warn("Unable to acquire lock for path {}", path);
    	throw new IllegalStateException("Unable to acquire lock");
    }
	
	@Override
	public void release(CloseableLockHandle handle) {
		try (handle) {
		} catch (Exception ignore) {
			//throw new RuntimeException(e);
		}
	}

	
	@Override
    public Optional<InputStream> getContent(PK key) {
    	Objects.requireNonNull(key, "key");
    	Objects.requireNonNull(key.hash(), "hash");
    	 
        try {
            return backingStore.retrieve(key.hash());
        } catch (IOException e) {
        	 LOGGER.error("Unable to retrieve hash={}", key.hash(), e);
            return Optional.empty();
        }
        
       
    }

    @Override
    public boolean exists(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return getLatestVersion(key.path()).isPresent();
    }

    
    @Override
    public boolean pathExists(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        return repositoryManager.pathExists(key.path());
    }
    
    // ----------------------------
 	// CREATE - create a new version with new UUID(storeId) , under path/{storeId}
 	// 
    @Override
    public Optional<BlobMeta> create(PK key, BlobMeta meta, InputStream content) throws IOException {
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
	public Optional<BlobMeta> create(PK key, BlobMeta meta, InputStream content, String id) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "path");
        Objects.requireNonNull(content, "content");
        return createNewVersion(key, meta, content, StoreOperation.Created, id);
	}

	@Override
    public Optional<BlobMeta> update(PK key, InputStream content) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(content, "content");

        String nodePath = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(nodePath);
        if (opt.isEmpty()) {
        	LOGGER.warn("Cannot update a non-exisitent path {}", nodePath);
        	return Optional.empty();
        }
        VersionMeta mx = opt.get();
        if (isDeleted(mx)) {
            LOGGER.warn("Cannot update a deleted path {}" , nodePath);
            return Optional.empty();
        }
        return appendVersion(content, StoreOperation.Updated, mx, key);
    }

    @Override
    public Optional<InputStream> read(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(nodePath);

        if (opt.isPresent()) {
        	VersionMeta mx = opt.get();
            if (!isDeleted(mx)) {
                return backingStore.retrieve(mx.hash());
            }
            LOGGER.info("Attempt to read a deleted blob at path {}", nodePath);
        }
        
        LOGGER.warn("No version of blob exists at path {}", nodePath);
        return Optional.empty();
    }

    @Override
    public Optional<BlobMeta> delete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(nodePath);

        if (opt.isEmpty()) {
            LOGGER.warn("Nothing to delete at path {}" , nodePath);
            return Optional.empty();
        }
        VersionMeta mx = opt.get();
        if (isDeleted(mx)) {
        	LOGGER.info("Attempt to delete an already deleted  path {}" , nodePath);
            return Optional.empty();
        }
        return appendVersion(null, StoreOperation.Deleted, mx, key);
    }

    @Override
    public Optional<BlobMeta> undelete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(nodePath);

        if (opt.isEmpty()) {
            LOGGER.warn("Unable to undelete {}" , nodePath);
            return Optional.empty();
        }
        VersionMeta mx = opt.get();

        if (!isDeleted(mx)) {
            LOGGER.warn("Can only undelete a deleted path " + nodePath);
        }
        return appendVersion(null, StoreOperation.Undeleted, mx, key);
    }

    
    @Override
    public Optional<BlobMeta> restore(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(key.hash(), "key.hash");

        String nodePath = PathUtils.normalizePath(key.path());
        String hash = key.hash();
        
        List<VersionMeta> l = getAllVersions(nodePath);
        
        Optional<VersionMeta> opt = l.stream().filter((m) -> hash.equals(m.hash())).findFirst();

        if (opt.isEmpty()) {
            LOGGER.warn("Unable to restore path {} hash {}", nodePath, hash);
            return Optional.empty();
        }
        
        VersionMeta mx = opt.get();

        
        return appendVersion(null, StoreOperation.Restored, mx, key);
    }
    
    
    @Override
    public Optional<VersionMeta> getLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        return repositoryManager.getLatestVersion(nodePath);
    }

    @Override
    public List<VersionMeta> getChildrenLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        return repositoryManager.getChildrenLatestVersion(nodePath);
     }

    @Override
    public List<String> getChildrenPath(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        return repositoryManager.getChildrenPath(nodePath);
    }

    @Override
    public List<VersionMeta> getAllVersions(String nodePath) throws IOException {
    	return repositoryManager.getAllVersions(nodePath);
    }
  
    private Optional<BlobMeta> createNewVersion(
            PK key,
            BlobMeta meta,
            InputStream content,
            StoreOperation operation
            
    ) throws IOException {
    	return createNewVersion(key, meta, content, operation, null);
    }
    
    
    private Optional<BlobMeta> createNewVersion(
            PK key,
            BlobMeta meta,
            InputStream content,
            StoreOperation operation,
            String forcedId
    ) throws IOException {

        final String id =
        (forcedId != null && !forcedId.isBlank()) ? forcedId : UUID.randomUUID().toString();
        
        String nodePath = PathUtils.normalizePath(key.path());
        
        nodePath = nodePath + "/" + id;
        
        // Save payload in immutable store, store is authority for attr/hash (same as FS) :contentReference[oaicite:13]{index=13}
        BlobMeta blobMeta = backingStore.save(meta, content).orElseThrow();

        persistAndPublish(nodePath, blobMeta, operation);
        
        return Optional.of(blobMeta);
    }

    private Optional<BlobMeta> appendVersion(
            InputStream content,
            StoreOperation operation,
            VersionMeta current,
            PK pkey
            
    ) throws IOException {

    	Objects.requireNonNull(current);
    	Objects.requireNonNull(pkey);
    	Objects.requireNonNull(operation);
        String nodePath = PathUtils.normalizePath(pkey.path());
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

        persistAndPublish(nodePath, blobMeta, operation);
        
        return Optional.of(blobMeta);
    }

    
    
    private void persistAndPublish(String nodePath, BlobMeta blobMeta,  StoreOperation operation) throws IOException {
    	//Capture global context/transaction context, if no active context then create a store context 
    	//with sane defaults
    	
    	//If no active gobal/transaction context StoreContext is just "operation" capture 
    	//read/write/update/restore etc with synthesize ids 
    	//transactionId is set to null and transactionResult is set to AUTO_COMMITTED--
    	StoreContext sc = StoreContext.create(operation.name());
        PathEvent pe = new PathEvent(nodePath,  sc);
        VersionMeta vm = new VersionMeta(blobMeta, pe);
        
        // repo is the authoritative write: insert the version row and move HEAD in one statement.
        // This keeps inode/version ids entirely within the persistence layer.
//        try (@SuppressWarnings("unused") ContextScope scope = Context.ensureContext(sc.operation() + "-" +sc.transactionResult());
//        		@SuppressWarnings("unused")	CloseableLockHandle lock = tryAcquireLock(nodePath)
//        				) {
	        if (AUTO_COMMITTED.equals(sc.transactionResult())) {
	        		repositoryManager.persistAndPublish(vm); 
	        } else if (IN_FLIGHT.equals(sc.transactionResult())) {
	        	// Under an explicit store transaction, this write must NOT be committed yet.
	        	// There is exactly one DB commit, and it is only legal at transaction finalization.
	        	//
	        	// - commit() transitions IN_FLIGHT → COMMITTED, then moves HEAD
	        	// - rollback() transitions IN_FLIGHT → ROLLED_BACK, no HEAD movement
	        	//
	        	// If this IN_FLIGHT write is ever made durable before explicit finalization,
	        	// that indicates an illegal early DB commit and is a bug / red flag.
        		repositoryManager.persist(vm);
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
