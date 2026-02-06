package com.veritynow.rest.api;

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.web.multipart.MultipartFile;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.PathMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.CloseableLockHandle;
import com.veritynow.core.store.versionstore.PathUtils;
import com.veritynow.core.store.versionstore.repo.RepositoryManager;
import com.veritynow.core.store.versionstore.repo.VersionMetaRepository;

import util.StringUtils;

@Service
public class ConsoleService {

	private final TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore;
	private final VersionMetaRepository versionMetaRepository;
	private final RepositoryManager repoManager;
	private static final Logger LOGGER = LogManager.getLogger();
	private final APIService apiService;

	public ConsoleService(
			TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore,
			VersionMetaRepository versionMetaRepository, RepositoryManager repoManager, APIService apiService) {
		this.versionStore = versionStore;
		this.versionMetaRepository = versionMetaRepository;
		this.repoManager = repoManager;
		this.apiService = apiService;
		LOGGER.info("Console Service using " + versionStore.getClass().getName());

	}

	public List<VersionMeta> getVersionsByTransactionId(String transactionId) {
		Objects.requireNonNull(transactionId, "transactionId");
		return versionMetaRepository.findByTransactionId(transactionId);
	}

	public List<VersionMeta> getVersionsByCorrelationId(String correlationId) {
		Objects.requireNonNull(correlationId, "correlationId");
		return versionMetaRepository.findByCorrelationId(correlationId);
	}

	public List<VersionMeta> getVersionsByCorrelationIdAndTransaction(String correlationId, String transactionId) {
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");
		return versionMetaRepository.findByCorrelationIdAndTransactionId(correlationId, transactionId);
	}

	public List<VersionMeta> getVersionsByWorkflowId(String workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");
		return versionMetaRepository.findByWorkflowId(workflowId);
	}

	public List<VersionMeta> getVersionsByWorkflowIdAndCorrelationId(String workflowId, String correlationId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		return versionMetaRepository.findByWorkflowIdAndCorrelationId(workflowId, correlationId);
	}

	public List<VersionMeta> getVersionsByWorkflowIdAndCorrelationIdAndTransationId(String workflowId,
			String correlationId, String transactionId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(correlationId, "transactionId");
		return versionMetaRepository.findByWorkflowIdAndCorrelationIdAndTransactionId(workflowId, correlationId,
				transactionId);
	}

	public List<VersionMeta> getWorkflows(String path) {
		Objects.requireNonNull(path, "path");
		return repoManager.getWorkflows(path);
	}

	public Optional<PathMeta> getPathMeta(String merklePath) {
		try {
			String p = normalizePath(merklePath);
			// --- HEAD (payload at this exact path, if any)
			List<String> children = versionStore.getChildrenPath(p);
			List<VersionMeta> versions = versionStore.getAllVersions(p);

			PathMeta nm = new PathMeta(p, children, versions);

			if (nm != null) {
				return Optional.of(nm);
			}

		} catch (Exception e) {
			LOGGER.error("Unable to get meta data {}", merklePath, e);
		}
		return Optional.empty();
	}

	private static String normalizePath(String path) {
		if (path == null || path.isBlank())
			return "/";
		String p = path.trim();
		if (!p.startsWith("/"))
			p = "/" + p;
		p = p.replaceAll("/+$", "");
		return p.isEmpty() ? "/" : p;
	}

	public Optional<List<VersionMeta>> getAllVersions(String nodePath) throws IOException {
		List<VersionMeta> l = versionStore.getAllVersions(nodePath);
		return Optional.of(l);
	}

	public Optional<InputStream> loadBytesByHash(String hash) throws IOException {
		Optional<InputStream> opt = versionStore.getContent(new PK(null, hash));
		if (opt.isPresent())
			return opt;
		return Optional.empty();
	}

	public Optional<BlobMeta> undelete(String path) {
		try {
			Optional<BlobMeta> opt = versionStore.undelete(new PK(path, null));
			BlobMeta m = opt.get();
			return Optional.of(m);
		} catch (IOException e) {
			LOGGER.error("Unable to delete {}", path, e);
		}
		return Optional.empty();
	}

	public void processTransaction(APITransaction apiTxn, String namespace, Map<String, MultipartFile> fileMap)
			throws Exception {

		List<Transaction> txns = apiTxn.transactions();
		List<String> paths = getPathsToLock(apiTxn.transactions(), namespace);

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

				// normalize and applyNamespace
				String path = PathUtils.normalizeAndApplyNamespace(txn.path(), namespace);

				try (is) {
					switch (op) {
					case "CREATE":
						// for create we make a new segment which is analogous to DB generated index
						String lastSegment = UUID.randomUUID().toString();
						String newPath = path + "/" + lastSegment;
						apiService.createExactPath(newPath, is, mimeType, name);
						continue;
					case "CREATE?exactPath":
						apiService.createExactPath(path, is, mimeType, name);
						continue;
					case "UPSERT?exactPath":
						if (versionStore.exists(new PK(path, null))) {
							apiService.update(path, is, mimeType, name);
						} else {
							apiService.createExactPath(path, is, mimeType, name);
						}
						continue;
					case "UPDATE":
						apiService.update(path, is, mimeType, name);
						continue;
					case "DELETE":
						apiService.delete(path, "");
						continue;
					case "UNDELETE":
						apiService.undelete(path);
						continue;
					default:
						if (op.startsWith("RESTORE?")) {
							// extract hash from op
							
							String[] parts = op.substring("RESTORE?".length()).split("=");
							Map<String, String> p = new HashMap<>();
							for (int i = 0; i  < parts.length - 2; i=i+2)
								p.put(parts[i], parts[i+1]);
							
							String hash = p.get("hash");
							String algo = p.get("algo");
							
							if (hash != null && algo != null) {
								apiService.restore(path, hash, algo);
								continue;
							} else if (hash != null) {
								apiService.restore(path, hash);
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
					LOGGER.info("Advisory lock - {}", l);
				});
			}
			
			versionStore.commit();
			
			
			List<Long> lcksNow = versionStore.findActiveAdvisoryLocks(txnId);
			
			if (!lcksBefore.isEmpty()  && !lcksNow.isEmpty()) {
				LOGGER.warn("Possible leak - advisory locks not released for transaction {}", txnId);
				lcksNow.stream().forEach((l) -> {
					LOGGER.info("Advisory lock - {}", l);
				});
			} else if (lcksBefore.isEmpty() && lcksNow.isEmpty()){
				LOGGER.info("No advisory locks was acquired");
			} else if (!lcksBefore.isEmpty() && lcksNow.isEmpty()){
				lcksBefore.stream().forEach((l) -> {
					LOGGER.info("Advisory lock - {} released", l);
				});
			}
		} catch (Throwable e) {
			String txnId = Context.transactionIdOrNull();
			versionStore.rollback();
			LOGGER.error("Transaction failed {}", txnId, e);
			throw new IOException("Transaction rolled back", e);
		}

	}

	private List<String> getPathsToLock(List<Transaction> transactions, String namespace) {
		Set<String> paths = new LinkedHashSet<>();

		for (Transaction txn : transactions) {
			// normalize and appylNamespace
			String path = PathUtils.normalizeAndApplyNamespace(txn.path(), namespace);

			paths.add(path);
		}

		return paths.stream().toList();
	}

}
