package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface  ImmutableBackingStore<T, META> extends Store<T, META> {
	
	public final static String JSON_EXTENSION = ".meta.json";
	
	public Optional<META> save(String name, String mimetype, InputStream is) throws IOException;
	public Optional<META> save(META meta, InputStream is) throws IOException;
	public Optional<InputStream> retrieve(String hash) throws IOException; 
	public boolean exists(String hash) throws IOException;
	
	
	public HashingService getHashingService();
}
