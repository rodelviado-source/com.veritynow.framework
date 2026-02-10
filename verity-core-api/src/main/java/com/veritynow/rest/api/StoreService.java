package com.veritynow.rest.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.threeten.bp.Instant;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.PathMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.CloseableLockHandle;
import com.veritynow.core.store.versionstore.PathUtils;

import util.StringUtils;

@Service
public class StoreService {

	
	private final TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore;

	private static final Logger LOGGER = LogManager.getLogger();
	
	public StoreService(
			TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore
			) {
		this.versionStore = versionStore;
		LOGGER.info("Store Service using " + versionStore.getClass().getName());
	}


	//retrieves all version including deleted ones
	public List<VersionMeta> getAllVersions(String path) throws IOException {
		return  versionStore.getAllVersions(path);
	}
	
	public Optional<VersionMeta> getLatestVersion(String path) throws IOException {
		
			Optional<VersionMeta> opt = versionStore.getLatestVersion(path);
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();

				if (StoreOperation.Deleted().equals(vm.operation())) {
					LOGGER.error("Attempt to get a deleted path {}", path);
					return Optional.empty();
				}
				return Optional.of(vm);
			}
		return Optional.empty();
	}
	
	public List<VersionMeta> getChildrenLatestVersion(String path) throws IOException {
		
			List<VersionMeta> bms = versionStore.getChildrenLatestVersion(path);
			return bms.stream().filter(bm -> !StoreOperation.Deleted().equals(bm.operation())).toList();
	}

	//interface to the hash keyed immutable store
	public Optional<InputStream> getContent(String hash) throws IOException {
		return  versionStore.getContent(new PK(null, hash));
	}

	//interface to the hash keyed immutable store
	public Optional<BlobMeta> getContentMeta(String hash) throws IOException {
		return versionStore.getContentMeta(new PK(null, hash));
	}
	
	public Optional<VersionMeta> create(String path, InputStream is, String mimeType, String name) throws IOException {
		Optional<BlobMeta> opt = versionStore.create(new PK(path, null), new BlobMeta(name, mimeType, 0), is);
		if (opt.isPresent()) {
			Optional<VersionMeta> latest = versionStore.getLatestVersion(path);
			return latest;
		}
		return Optional.empty();
	}
	
	public Optional<VersionMeta> createExactPath(String path, InputStream is, String mimeType, String name) throws IOException {
			String parent = path.substring(0, path.lastIndexOf("/"));
			String lastSegment = lastSegment(path);
			Optional<BlobMeta> opt = versionStore.create(new PK(parent, null), new BlobMeta(name, mimeType, 0), is, lastSegment);

			if (opt.isPresent()) {
				return versionStore.getLatestVersion(path);
			}
		return Optional.empty();
	}

	public Optional<VersionMeta> update(String path, InputStream is, String mimeType, String name) throws IOException {
		
			Optional<BlobMeta> opt = versionStore.update(new PK(path, null), is);
			if (opt.isPresent())
				return versionStore.getLatestVersion(path);
		return Optional.empty();
	}
	
	public Optional<BlobMeta> delete(String path, String reason) throws IOException {
		return versionStore.delete(new PK(path, null));
	}

	public Optional<VersionMeta> undelete(String path) throws IOException {
		
		Optional<BlobMeta> bm = versionStore.undelete(new PK(path, null));
		if (bm.isPresent()) {
			return versionStore.getLatestVersion(path);
		}
		return Optional.empty();
	}
	
	public Optional<VersionMeta> restore(String path, String hash) throws IOException {

			Optional<BlobMeta> bm = versionStore.restore(new PK(path, hash));
			if (bm.isPresent()) {
				return versionStore.getLatestVersion(path);
			}
		return Optional.empty();
	}

	
	public Optional<VersionMeta> restore(String path, String hash, String algo) throws IOException {

		//At the present parameter algo is ignored, the store's default algo is used instead
		Optional<BlobMeta> bm = versionStore.restore(new PK(path, hash));
		if (bm.isPresent()) {
			return versionStore.getLatestVersion(path);
		}
	return Optional.empty();
}
	
	
	public void process() {
		
	}
	
	public void processTransaction(APITransaction apiTxn, Map<String, MultipartFile> fileMap)
			throws Exception {

		long startTime = System.nanoTime();
		
		List<Transaction> txns = apiTxn.transactions();
		List<String> paths = getPathsToLock(apiTxn.transactions());

		// create a context if not present, then acquire a lock
		try (ContextScope scope = Context.ensureContext("Transactional API");
				CloseableLockHandle lock = versionStore.tryAcquireLock(paths, 5, 50);) {

			if (scope == null) {
				LOGGER.error("Context is missing {}", paths);
				throw new Exception("No Context");
			}
			if (lock == null) {
				LOGGER.error("Cannot acquire lock for paths {}", paths);
				throw new Exception("Cannot acquire a lock");
			}
			
			versionStore.begin();
			String txnId = Context.transactionIdOrNull();
			for (Transaction txn : txns) {
				String op = txn.operation();
				String bref = txn.blobRef();
				String name = bref;
				String mimeType = "application/octet-stream";
				if (bref != null && fileMap.get(bref) != null) {
					MultipartFile mp = fileMap.get(bref);
					mimeType = mp.getContentType();
					name = StringUtils.isEmpty(mp.getOriginalFilename())
							? (StringUtils.isEmpty(mp.getName()) ? bref : mp.getName())
							: mp.getOriginalFilename();

				}

				InputStream is = apiTxn.blobs().get(bref);
				String path = txn.path();
				
				try (is) {
					switch (op) {
					case "CREATE":
						// for create we make a new segment which is analogous to DB generated index
						String lastSegment = UUID.randomUUID().toString();
						String newPath = path + "/" + lastSegment;
						createExactPath(newPath, is, mimeType, name);
						continue;
					case "CREATE?exactPath":
						createExactPath(path, is, mimeType, name);
						continue;
					case "UPSERT" : 
					case "UPSERT?exactPath" :
						if (versionStore.exists(new PK(path, null))) {
							update(path, is, mimeType, name);
						} else {
							createExactPath(path, is, mimeType, name);
						}
						continue;
					case "UPDATE":
						update(path, is, mimeType, name);
						continue;
					case "DELETE":
						delete(path, "");
						continue;
					case "UNDELETE":
						undelete(path);
						continue;
					default:
						if (op.startsWith("RESTORE?")) {
							// extract hash from op
							
							String[] parts = op.substring("RESTORE?".length()).split("=");
							Map<String, String> p = new HashMap<>();
							for (int i = 0; i  < parts.length - 1; i=i+2)
								p.put(parts[i], parts[i+1]);
							
							String hash = p.get("hash");
							String algo = p.get("algo");
							
							if (hash != null && algo != null) {
								//right now algo is ignored, store default is used
								//will support multi-algo, in the future
								restore(path, hash, algo);
								continue;
							} else if (hash != null) {
								restore(path, hash);
								continue;
							} else {
								throw new IllegalArgumentException("Invalid or missing operation parameters" + op);
							}
						}
						throw new IllegalArgumentException("Invalid Operation " + op);
					}
					
				}
			}
			
			List<Long> lcksBefore = versionStore.findActiveAdvisoryLocks(txnId);
			
			if (!lcksBefore.isEmpty()) {
				lcksBefore.stream().forEach((l) -> {
					LOGGER.info("Advisory lock acquired : {}", l);
				});
			}
			
			versionStore.commit();
			
			
			List<Long> lcksNow = versionStore.findActiveAdvisoryLocks(txnId);
			
		    if (lcksBefore.isEmpty() && lcksNow.isEmpty()){
				LOGGER.info("No advisory locks was acquired by transaction {}",txnId);
			} else {
				lcksBefore.stream().forEach((l) -> {
					if (lcksNow.contains(l)) {
						lcksNow.remove(lcksNow.indexOf(l));
						LOGGER.warn("Leak detected - Advisory lock was not released : {}", l);
					} else {
						LOGGER.warn("Advisory lock released : {}", l);
					}
				});
				
				lcksNow.stream().forEach((l) -> {
					LOGGER.warn("Possible leak - Transient advisory lock detected : {}", l);
				});
			}
		    long endTime = System.nanoTime();
		    
		    long durationInNano = endTime - startTime;
	        long durationInMilli = durationInNano / 1_000_000; // Convert nanoseconds to milliseconds
	        
		    LOGGER.info("Transaction completed in [{} ns] = [{} ms]", durationInNano, durationInMilli);
		} catch (Throwable e) {
			String txnId = Context.transactionIdOrNull();
			versionStore.rollback();
			LOGGER.error("Transaction failed {}", txnId, e);
			throw new IOException("Transaction rolled back", e);
		}
	}
	
	
	public Optional<PathMeta> getPathMeta(String path) {
		try {
			String p = PathUtils.normalizePath(path);
			// --- HEAD (payload at this exact path, if any)
			List<String> children = versionStore.getChildrenPath(p);
			List<VersionMeta> versions = versionStore.getAllVersions(p);

			PathMeta nm = new PathMeta(p, children, versions);

			if (nm != null) {
				return Optional.of(nm);
			}

		} catch (Exception e) {
			LOGGER.error("Unable to get meta data {}", path, e);
		}
		return Optional.empty();
	}
	
	
	private static String lastSegment(String path) {
		int i = path.lastIndexOf('/');
		return (i >= 0) ? path.substring(i + 1) : path;
	}
	
	private List<String> getPathsToLock(List<Transaction> transactions) {
		Set<String> paths = new LinkedHashSet<>();

		for (Transaction txn : transactions) {
			paths.add(txn.path());
		}
		return paths.stream().sorted().toList();
	}

}
