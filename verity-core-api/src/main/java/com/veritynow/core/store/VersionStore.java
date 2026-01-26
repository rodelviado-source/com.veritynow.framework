package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface  VersionStore<PK, BLOBMETA, VERSIONMETA> extends Store<PK, BLOBMETA> {
	
	//Path may contain many versions of a blob, get the latest
	Optional<VERSIONMETA> getLatestVersion(String path) throws IOException;
	
	//Path may contain many versions of a blob, get all
	List<VERSIONMETA> getAllVersions(String path) throws IOException;
	
	// list all latest versions of blobs directly under this path
	List<VERSIONMETA> list(String path) throws IOException;
	
	List<String> listChildren(String path) throws IOException;
	
	Optional<InputStream> getContent(PK key);
	
}
