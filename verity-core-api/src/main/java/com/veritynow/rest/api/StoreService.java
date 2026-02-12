package com.veritynow.rest.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

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

@Service
public class StoreService {

	private final TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore;

	private static final Logger LOGGER = LogManager.getLogger();

	public StoreService(
			TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore) {
		this.versionStore = versionStore;
		LOGGER.info("Store Service using " + versionStore.getClass().getName());
	}

	// retrieves all version including deleted ones
	public List<VersionMeta> getAllVersions(String path) throws IOException {
		return versionStore.getAllVersions(PK.path(path));
	}

	public Optional<VersionMeta> getLatestVersion(String path) throws IOException {

		Optional<VersionMeta> opt = versionStore.getLatestVersion(PK.path(path));
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			if (StoreOperation.Deleted().equals(vm.operation())) {
				LOGGER.error("Attempt to get a deleted path {}", path);
				return Optional.empty();
			}
			return opt;
		}
		return Optional.empty();
	}

	
	public List<VersionMeta> getChildrenLatestVersion(String path) throws IOException {

		List<VersionMeta> bms = versionStore.getChildrenLatestVersion(PK.path(path));
		return bms.stream().filter(bm -> !StoreOperation.Deleted().equals(bm.operation())).toList();
	}

	// interface to the hash keyed immutable store
	public Optional<InputStream> getContent(String hash) throws IOException {
		return versionStore.getContent(PK.hash(hash));
	}

	// interface to the hash keyed immutable store
	public Optional<BlobMeta> getContentMeta(String hash) throws IOException {
		return versionStore.getContentMeta(PK.hash(hash));
	}

	public Optional<VersionMeta> create(String path, InputStream is, String mimeType, String name) throws IOException {
		return versionStore.create(PK.path(path), new BlobMeta(name, mimeType), is);
	}

	public Optional<VersionMeta> createExactPath(String path, InputStream is, String mimeType, String name)
			throws IOException {
		String parent = path.substring(0, path.lastIndexOf("/"));
		String lastSegment = lastSegment(path);
		return versionStore.create(PK.path(parent), new BlobMeta(name, mimeType), is,
				lastSegment);
	}

	public Optional<VersionMeta> update(String path, InputStream is, String mimeType, String name) throws IOException {
		return versionStore.update(PK.path(path), is);
	}

	public Optional<VersionMeta> delete(String path, String reason) throws IOException {
		return versionStore.delete(PK.path(path));
	}

	public Optional<VersionMeta> undelete(String path) throws IOException {
		return versionStore.undelete(PK.path(path));
	}

	public Optional<VersionMeta> restore(String path, String hash) throws IOException {
		return versionStore.restore(new PK(path, hash));
	}

	public Optional<VersionMeta> restore(String path, String hash, String algo) throws IOException {
		// At the present parameter algo is ignored, the store's default algo is used
		// instead
		return restore(path, hash);
	}

	public Optional<VersionMeta> process(String op, String path, BlobMeta bm , InputStream is) throws Exception {
		
		String name = bm.name();
		String mimeType = bm.mimeType();

		
			switch (op) {
			case "CREATE":
				// for create we make a new segment which is analogous to DB generated index
				String lastSegment = UUID.randomUUID().toString();
				String newPath = path + "/" + lastSegment;
				Objects.requireNonNull(is);
				return createExactPath(newPath, is, mimeType, name);
			case "CREATE?exactPath":
				Objects.requireNonNull(is);
				return createExactPath(path, is, mimeType, name);
			case "UPSERT":
			case "UPSERT?exactPath":
				Objects.requireNonNull(is);
				if (versionStore.exists(PK.path(path))) {
					return update(path, is, mimeType, name);
				} else {
					return createExactPath(path, is, mimeType, name);
				}
			case "UPDATE":
				Objects.requireNonNull(is);
				return update(path, is, mimeType, name);
			case "DELETE":
				return delete(path, "");
			case "UNDELETE":
				return undelete(path);
			default:
				if (op.startsWith("RESTORE?")) {
					// extract hash from op

					String[] parts = op.substring("RESTORE?".length()).split("=");
					Map<String, String> p = new HashMap<>();
					for (int i = 0; i < parts.length - 1; i = i + 2)
						p.put(parts[i], parts[i + 1]);

					String hash = p.get("hash");
					String algo = p.get("algo");

					if (hash != null && algo != null) {
						// right now algo is ignored, store default is used
						// will support multi-algo, in the future
						return restore(path, hash, algo);
						
					} else if (hash != null) {
						return restore(path, hash);
					} else {
						throw new IllegalArgumentException("Invalid or missing operation parameters" + op);
					}
				}
				throw new IllegalArgumentException("Invalid Operation " + op);
			}
	}

	public List<VersionMeta> processTransaction(APITransaction apiTxn, Map<String, BlobMeta> metas) throws Exception {

		long startTime = System.nanoTime();

		String txnId = null;
		List<Transaction> txns = apiTxn.transactions();
		List<String> paths = getPathsToLock(apiTxn.transactions());

		// create a context if not present, then acquire a lock
		try (ContextScope scope = Context.ensureContext("Transactional API");
				CloseableLockHandle lock = versionStore.tryAcquireLock(paths, 5, 50);) {

			
			if (scope == null) {
				LOGGER.error("Context is missing {}", paths);
				throw new Exception("No Context");
			}
			
			txnId = Context.transactionIdOrNull();
			
			if (lock == null) {
				LOGGER.error("Cannot acquire lock for paths {}", paths);
				throw new Exception("Cannot acquire a lock");
			}
			
			versionStore.begin();
			
			ArrayList<VersionMeta> vms = new ArrayList<>();
			
			for (Transaction txn : txns) {
				String op = txn.operation();
				String bref = txn.blobRef();
				String name = bref;
				
				String mimeType = "application/octet-stream";
				BlobMeta bm = metas.get(bref);
				if (bref != null && bm != null) {
					mimeType = bm.mimeType();
					name = bm.name();
				}

				InputStream is = apiTxn.blobs().get(bref);
				String path = txn.path();
				try (is) {
					Optional<VersionMeta> opt = process(op, path, new BlobMeta(name, mimeType), is);
					if (opt.isPresent()) {
						vms.add(opt.get());
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

			if (lcksBefore.isEmpty() && lcksNow.isEmpty()) {
				LOGGER.info("No advisory locks was acquired by transaction {}", txnId);
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
			return vms;
		} catch (Throwable e) {
			
			versionStore.rollback();
			LOGGER.error("Transaction failed {}", txnId, e);
			throw new IOException("Transaction rolled back", e);
		}
	}

	public Optional<PathMeta> getPathMeta(String path) {
		try {
			String p = PathUtils.normalizePath(path);
			// --- HEAD (payload at this exact path, if any)
			List<String> children = versionStore.getChildrenPath(PK.path(p));
			List<VersionMeta> versions = versionStore.getAllVersions(PK.path(p));

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
