package com.veritynow.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.veritynow.core.store.VersionStore;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.PathMeta;
import com.veritynow.core.store.meta.VersionMeta;

import jakarta.transaction.Transactional;


@Service
public class ConsoleService {

	private final VersionStore<PK, BlobMeta, VersionMeta> versionStore;
	private static final Logger LOGGER = LogManager.getLogger();
	
	
	public ConsoleService(VersionStore<PK, BlobMeta, VersionMeta> versionStore) {
		this.versionStore = versionStore;
		LOGGER.info("Console Service using " + versionStore.getClass().getName());
		
	}
	
	

	public Optional<PathMeta> getPathMeta(String merklePath) {
		try {
		String p = normalizePath(merklePath);	
		// --- HEAD (payload at this exact path, if any)
		List<String> children = versionStore.listChildren(p);
		List<VersionMeta> versions = versionStore.getAllVersions(p);
		
		PathMeta nm = new PathMeta(p, children, versions ); 
		
		if (nm != null) {
			return Optional.of(nm);
		}
		
		} catch (Exception e) {
			LOGGER.error("Unable to get meta data {}", merklePath , e);
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


	public Optional<List<VersionMeta>> listAllVersions(String nodePath) {
		try {
			return Optional.of(versionStore.getAllVersions(nodePath));
		} catch (IOException e) {
			LOGGER.error("Can't get versions {}", nodePath, e);
		}
		return Optional.empty();
	}

	public Optional<InputStream> loadBytesByHash(String hash) {
		Optional<InputStream> opt = versionStore.getContent(new PK(null,hash));
		if (opt.isPresent()) {
			return opt;
		}
		return Optional.empty();
	}

	@Transactional
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
	
}
