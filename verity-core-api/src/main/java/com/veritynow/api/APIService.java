package com.veritynow.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.CloseableLockHandle;

import util.JSON;


@Service
public class APIService {

	private final TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore;
	private static final Logger LOGGER = LogManager.getLogger();

	public APIService(TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore) {
		this.versionStore = versionStore;
		LOGGER.info("API Service using " + versionStore.getClass().getName());
	}

	@Transactional
	public Optional<VersionMeta> create(String parentPath, InputStream is, String mimeType, String name) throws IOException {

	
		Optional<BlobMeta> opt = versionStore.create(new PK(parentPath, null), new BlobMeta(name, mimeType, 0), is);
		if (opt.isPresent()) {
			Optional<VersionMeta> latest = versionStore.getLatestVersion(parentPath);
			return latest;
		}

	
		return Optional.empty();
	}

	@Transactional
	public Optional<VersionMeta> createExactPath(String path, InputStream is, String mimeType, String name) throws IOException {

		
			String parent = path.substring(0, path.lastIndexOf("/"));
			String lastSegment = lastSegment(path);
			Optional<BlobMeta> opt = versionStore.create(new PK(parent, null), new BlobMeta(name, mimeType, 0), is, lastSegment);

			if (opt.isPresent()) {
				return versionStore.getLatestVersion(path);
			}
		
		return Optional.empty();
	}

	@Transactional
	public Optional<VersionMeta> update(String identityPath, InputStream is, String mimeType, String name) throws IOException {
		
			Optional<BlobMeta> opt = versionStore.update(new PK(identityPath, null), is);
			if (opt.isPresent())
				return versionStore.getLatestVersion(identityPath);
		
		return Optional.empty();
	}

	public Optional<InputStream> getLatestVersion(String path) throws IOException {
		
			Optional<VersionMeta> opt = versionStore.getLatestVersion(path);
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();

				if (StoreOperation.Deleted().equals(vm.operation())) {
					LOGGER.error("Attempt to get a deleted path {}", path);
					return Optional.empty();
				}
				Optional<InputStream> optIS = versionStore.read(new PK(path, vm.hash()));
				if (optIS.isPresent()) {
					return Optional.of(optIS.get());
				}
				try {
					LOGGER.error("Meta exists but unable to get Payload for VersionMeta {}",
							JSON.MAPPER.writeValueAsString(vm));
				} catch (Exception e) {
					LOGGER.error("Unable to deserialize meta", e);
				}
			}

		return Optional.empty();
	}

	public List<VersionMeta> list(String path) throws IOException {
		
			List<VersionMeta> bms = versionStore.getChildrenLatestVersion(path);
			return bms.stream().filter(bm -> !StoreOperation.Deleted().equals(bm.operation())).toList();
		
	}

	@Transactional
	public Optional<BlobMeta> delete(String path, String reason) throws IOException {
		Optional<BlobMeta> bm = versionStore.delete(new PK(path, null));
		if (bm.isPresent()) {
			return Optional.of(bm.get());
		}
		return Optional.empty();
	}

	@Transactional
	public Optional<VersionMeta> undelete(String path) throws IOException {
		
		Optional<BlobMeta> bm = versionStore.undelete(new PK(path, null));
		if (bm.isPresent()) {
			return versionStore.getLatestVersion(path);
		}
		return Optional.empty();
	}

	@Transactional
	public Optional<VersionMeta> restore(String path, String hash) throws IOException {

			Optional<BlobMeta> bm = versionStore.restore(new PK(path, hash));
			if (bm.isPresent()) {
				return versionStore.getLatestVersion(path);
			}
		return Optional.empty();
	}

	

	private static String lastSegment(String path) {
		int i = path.lastIndexOf('/');
		return (i >= 0) ? path.substring(i + 1) : path;
	}

}
