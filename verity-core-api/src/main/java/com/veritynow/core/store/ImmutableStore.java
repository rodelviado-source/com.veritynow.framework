package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface  ImmutableStore<KEY, META> extends Store<KEY, META> {
	
		
	public Optional<META> save(String name, String mimetype, InputStream is) throws IOException;
	public Optional<META> save(META meta, InputStream is) throws IOException;
	public Optional<META> getMeta(KEY key) throws IOException; 
	public Optional<InputStream> retrieve(KEY key) throws IOException; 
	public boolean exists(KEY key) throws IOException;
	
	
	public HashingService getHashingService();
}
