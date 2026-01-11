package com.veritynow.v2.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface  VersionStore<PK, BLOBMETA, VERSIONMETA> extends Store<PK, BLOBMETA> {
	
	//Path may contain many versions, get the latest
	Optional<VERSIONMETA> getLatestVersion(String path) throws IOException;
	
	//Path may contain many versions, get all
	List<VERSIONMETA> getAllVersions(String path) throws IOException;
	
	//list all latest versions
	List<VERSIONMETA> list(String nodePath) throws IOException;
	
	List<String> listChildren(String nodePath) throws IOException;
	
	Optional<InputStream> getByHash(String hash);
	
	Optional<BLOBMETA> create(PK key, BLOBMETA blob, InputStream in, String id) throws IOException;
	
}
