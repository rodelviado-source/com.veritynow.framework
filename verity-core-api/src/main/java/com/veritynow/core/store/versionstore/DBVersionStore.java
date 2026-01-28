package com.veritynow.core.store.versionstore;

import static com.veritynow.core.store.txn.TransactionResult.AUTO_COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
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
import com.veritynow.core.store.versionstore.model.DirEntry;
import com.veritynow.core.store.versionstore.repo.RepositoryManager;

import util.DBUtil;


public class DBVersionStore extends AbstractStore<PK, BlobMeta> implements  TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope>  {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final ImmutableBackingStore<String, BlobMeta> backingStore;
    private final ContextAwareTransactionManager txnManager;
    LockingService lockingService;
    
    
    private final Publisher publisher;
	private final RepositoryManager repositoryManager;

    public DBVersionStore(
            ImmutableBackingStore<String, BlobMeta> backingStore,
			DSLContext dsl,
			RepositoryManager repositoryManager,
            ContextAwareTransactionManager txnManager,
            LockingService lockingService
    ) {
   
    	super(backingStore.getHashingService());
        this.backingStore = backingStore;
        this.txnManager = txnManager;
		this.lockingService = lockingService;
		this.publisher  = new Publisher(dsl, lockingService, repositoryManager);
		this.repositoryManager = Objects.requireNonNull(repositoryManager, "repositoryManager required");
    	repositoryManager.ensureRootInode();
        
        if (publisher.isLockingCapable()) {
        	DBUtil.ensureProjectionReady(dsl);
        }	
        
        LOGGER.info(
        		"Inode-backed Versioning Store started. Using {} for Immutable Storage", backingStore.getClass().getName()
      	);
    }
    
 
    //convenience if transaction support is avialable
    @Override
	public Optional<ContextScope> begin() {
    	if (txnManager != null)
    		return txnManager.begin();
    	return Optional.empty();
	}

    @Override
	public void commit() {
    	if (txnManager != null && Context.isActive())
    		txnManager.commit();
	}

	@Override
	public void rollback() {
		if (txnManager != null && Context.isActive())
			txnManager.rollback();
	}

	@Override
	public Optional<LockHandle> acquire(List<String> paths) {
		if (lockingService != null)
			return Optional.of(lockingService.acquire(paths));
		return Optional.empty();
	}

	@Override
	public Optional<LockHandle> acquireLock(String... paths) {
		return acquire(List.of(paths));
	}


	@Override
	public void release(LockHandle handle) {
		if (lockingService != null) 
			lockingService.release(handle);
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

    // ----------------------------
 	// CREATE - create a new version with new UUID(storeId) , under path/{storeId}
 	// 
    @Override
    @Transactional
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
    @Transactional
	public Optional<BlobMeta> create(PK key, BlobMeta meta, InputStream content, String id) throws IOException {
    	Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "path");
        Objects.requireNonNull(content, "content");
        return createNewVersion(key, meta, content, StoreOperation.Created, id);
	}

	@Override
    @Transactional
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
    @Transactional(readOnly = true)
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
    @Transactional
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
    @Transactional
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

    
    @Transactional
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
    @Transactional(readOnly = true)
    public Optional<VersionMeta> getLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeIdOpt = repositoryManager.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return Optional.empty();
        
        Long inodeId = inodeIdOpt.get();
        return getLatestVersionByInodeId(inodeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionMeta> list(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = repositoryManager.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();
        List<VersionMeta> out = new ArrayList<>();
        Optional<VersionMeta> vmOpt;
        List<DirEntry> children = repositoryManager.findAllByParentId(inodeId);
        for (DirEntry child : children) {
            Long childId = child.child().id();
            vmOpt = getLatestVersionByInodeId(childId);
            if (vmOpt.isPresent()) out.add(vmOpt.get());
        }
        
        return out;
     }

    @Override
    @Transactional(readOnly = true)
    public List<String> listChildren(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = repositoryManager.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();
        String np = PathUtils.trimEndingSlash(nodePath);

        List<DirEntry> children = repositoryManager.findAllByParentId(inodeId);
        return children.stream()
                .map(de -> np + "/" + de.name())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionMeta> getAllVersions(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = repositoryManager.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();

        return repositoryManager.findAllByInodeIdOrderByTimestampDescIdDesc(inodeId);
    
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
    	StoreContext sc = StoreContext.create(operation.name());
        PathEvent pe = new PathEvent(nodePath,  sc);

        // Publish is the authoritative write: insert the version row and move HEAD in one statement.
        // This keeps inode/version ids entirely within the persistence layer.
        VersionMeta vm = new VersionMeta(blobMeta, pe);

        if (AUTO_COMMITTED.equals(sc.transactionResult())) {
            if (publisher.isLockingCapable()) {
                publisher.acquireLockAndPublish(vm);
            } else {
                // Degraded mode: only allowed if the current head is also unfenced.
                publisher.publish(vm);
            }
        } else {
        	if (IN_FLIGHT.equals(sc.transactionResult())) {
        		// Transaction layer handles IN_FLIGHT -> COMMITTED/ROLLED_BACK and HEAD movement.
        		repositoryManager.saveVersionMeta(vm);
        	} else {
        		throw new IllegalStateException("Expecting " + IN_FLIGHT + "  got " + sc.transactionResult() + " instead");
        	}
            
        }
    }
 

    private Optional<VersionMeta> getLatestVersionByInodeId(Long id) {
    	return repositoryManager.getLatestVersionInodeId(id);
    }

    
    private static boolean isDeleted(VersionMeta m ) {
    	return StoreOperation.Deleted().equals(m.operation());
    }
   
}
