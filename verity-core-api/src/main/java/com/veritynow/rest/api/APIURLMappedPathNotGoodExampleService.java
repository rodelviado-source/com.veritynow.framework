package com.veritynow.rest.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.veritynow.core.store.meta.VersionMeta;


@Service
public class APIURLMappedPathNotGoodExampleService {

	private static final Logger LOGGER = LogManager.getLogger();
    private final StoreService storeService;
	
	public APIURLMappedPathNotGoodExampleService(StoreService storeService) {
		this.storeService = storeService;
		LOGGER.info("API URL mapped PATH Not Good Example Service using " + storeService.getClass().getName());
	}

	
	public Optional<VersionMeta> create(String parentPath, InputStream is, String mimeType, String name) throws IOException {
			return storeService.create(parentPath, is, mimeType, name);
	}

	
	public Optional<VersionMeta> createExactPath(String path, InputStream is, String mimeType, String name) throws IOException {
			return storeService.createExactPath(path, is, mimeType, name);
	}

	
	public Optional<VersionMeta> update(String identityPath, InputStream is, String mimeType, String name) throws IOException {
		return storeService.update(identityPath, is, mimeType, name);
	}

	public Optional<VersionMeta> getLatestVersion(String path) throws IOException {
		return storeService.getLatestVersion(path);
	}

	public List<VersionMeta> getChildrenLatestVersion(String path) throws IOException {
		return storeService.getChildrenLatestVersion(path);		
	}

	
	public Optional<VersionMeta> delete(String path, String reason) throws IOException {
		return storeService.delete(path, reason);
	}

	
	public Optional<VersionMeta> undelete(String path) throws IOException {
		return storeService.undelete(path);		
	}

	
	public Optional<VersionMeta> restore(String path, String hash) throws IOException {
		return storeService.restore(path, hash);
	}

	
	public Optional<VersionMeta> restore(String path, String hash, String algo) throws IOException {
		return storeService.restore(path, hash, algo);
	}
	

}
