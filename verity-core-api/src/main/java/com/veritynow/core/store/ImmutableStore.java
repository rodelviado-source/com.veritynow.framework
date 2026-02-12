package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface  ImmutableStore<KEY, I, O> extends Store<KEY, I, O> {
	
		
	public Optional<O> save(String name, String mimetype, InputStream is) throws IOException;
	public Optional<O> save(I meta, InputStream is) throws IOException;
	public Optional<O> getMeta(KEY key) throws IOException; 
	public Optional<InputStream> retrieve(KEY key) throws IOException; 
	public boolean exists(KEY key) throws IOException;
	
	
	public HashingService getHashingService();
}
