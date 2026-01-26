package com.veritynow.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.VersionStore;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;

import org.springframework.transaction.annotation.Transactional;
import util.JSON;

@Service
public class APIService {

	private final VersionStore<PK, BlobMeta, VersionMeta> versionStore;
	private static final Logger LOGGER = LogManager.getLogger();
	
	public APIService(VersionStore<PK, BlobMeta, VersionMeta> versionStore) {
		this.versionStore = versionStore;
		LOGGER.info("API Service using " + versionStore.getClass().getName());
	}

	@Transactional
	public Optional<VersionMeta> create(String parentPath, InputStream is, String mimeType, String name) {
		 try {
			 
			Optional<BlobMeta> opt = versionStore.create(new PK(parentPath,  null), new BlobMeta(name, mimeType,  0), is);
			if (opt.isPresent()) 
				return versionStore.getLatestVersion(parentPath);
			
		} catch (IOException e) {
			LOGGER.error("Unable to create {} {}", parentPath, e);
		}
		return Optional.empty(); 
	}
	
	@Transactional
	public Optional<VersionMeta> createExactPath(String path, InputStream is, String mimeType, String name) {
		 try {
			String parent = path.substring(0,path.lastIndexOf("/"));
			String lastSegment = lastSegment(path) ;
			
			Optional<BlobMeta> opt = versionStore.create(new PK(parent,  null), new BlobMeta(name, mimeType,  0), is, lastSegment);
			
			if (opt.isPresent()) {
				return versionStore.getLatestVersion(path);
			}	
		} catch (IOException e) {
			LOGGER.error("Unable to create {} {}", path, e);
		}
		return Optional.empty(); 
	}

	@Transactional
	public Optional<VersionMeta> update(String identityPath, InputStream is, String mimeType, String name) {
		try {
			Optional<BlobMeta> opt = versionStore.update(new PK(identityPath, null), is);
			if (opt.isPresent())
				return versionStore.getLatestVersion(identityPath);
		} catch (IOException e) {
			LOGGER.error("Unable to update {}", identityPath, e);
		}
		return Optional.empty();
	}

	public Optional<InputStream> get(String path) {
		try {
		Optional<VersionMeta> opt = versionStore.getLatestVersion(path);
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			
			if (StoreOperation.Deleted().equals(vm.operation()))
			{
				LOGGER.error("Attempt to get a deleted path {}",  path);
				return Optional.empty();
			}
			Optional<InputStream> optIS = versionStore.read(new PK(path, vm.hash()));
			if (optIS.isPresent()) {
				return Optional.of(optIS.get());
			}
			try {
				LOGGER.error("Meta exists but unable to get Payload for VersionMeta {}",  JSON.MAPPER.writeValueAsString(vm));
			} catch (Exception e) {
				LOGGER.error("Unable to deserialize meta",  e);
			}
		}
	}  catch (Exception e) {
		LOGGER.error("Unable to get {}",  path, e);
	}
		
	return Optional.empty();
	}


	public List<VersionMeta> list(String path) {
		try {
			 List<VersionMeta> bms = versionStore.list(path);
			return bms.stream()
	                 .filter(bm -> !StoreOperation.Deleted().equals(bm.operation()))
	                 .toList();
		} catch (Exception e) {
			throw new RuntimeException("Unable to list " + path, e);
		}
	}

	@Transactional
	public Optional<BlobMeta> delete(String path, String reason) {
		try {
			return versionStore.delete(new PK(path, null));
		} catch (IOException e) {
			LOGGER.error("Unable to delete {}", path, e);
		}
		return Optional.empty();
	}

	
	@Transactional
	public Optional<VersionMeta> undelete(String path) {
		try {
			Optional<BlobMeta> bm = versionStore.undelete(new PK(path, null));
			if (bm.isPresent()) {
				return versionStore.getLatestVersion(path);
			}

		} catch (IOException e) {
			LOGGER.error("Unable to undelete {}", path, e);
		}
		return Optional.empty();
	}
	
	@Transactional
	public Optional<VersionMeta> restore(String path, String hash) {
		try {
			 Optional<BlobMeta> bm = versionStore.restore(new PK(path, hash));
			 if (bm.isPresent()) {
				 return versionStore.getLatestVersion(path);
			 }
		} catch (IOException e) {
			LOGGER.error("Unable to restore {}?restore={}" , path ,  hash, e);
		}
		return Optional.empty();
	}
	
	
	
	

	private static String lastSegment(String path) {
		int i = path.lastIndexOf('/');
		return (i >= 0) ? path.substring(i + 1) : path;
	}
}
