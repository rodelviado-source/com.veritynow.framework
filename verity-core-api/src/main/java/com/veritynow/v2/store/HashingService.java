package com.veritynow.v2.store;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public interface HashingService {
	
	//compute the hash of content. 
	//cacheContent - cache the content if set to true
	String hash(InputStream content, boolean cacheContent);
	
	//compute the hash of content specified by the file path
	String hash(Path contentPath);
		
	//get current content hash
	Optional<String> hash();
	//get current content size
	Optional<Long> size();
	
	//first BUFFER_SIZE content, useful for file type discovery
	Optional<byte[]> header();
	
	//Get an InputStream for reading the cached content 
	Optional<InputStream> getInputStream(boolean deleteOnClose);
	
	void setBufferSize(int bs);
	int getBufferSize();

}