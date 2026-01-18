package com.veritynow.v2.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Implementors can implement a subset of the Operations
 * Can use a backing store that stores in any format
 * Can use any Indexing strategy
 * 
 */
public interface   Store<KEY, META> {
	
	public record  KV<META>(META meta, InputStream inputStream)  {}
	
	Optional<META> create(KEY key, META meta, InputStream in) throws IOException;
	Optional<META> create(KEY key, META blob, InputStream in, String id) throws IOException;
	
	Optional<InputStream> read(KEY key) throws IOException;
	Optional<META> update(KEY key, InputStream is) throws IOException;
	Optional<META> delete(KEY key) throws IOException;
	Optional<META> undelete(KEY key)throws IOException;
	Optional<META> restore(KEY key)throws IOException;
	
	List<META> bulkCreate(Map<KEY, KV<META>> kais) throws IOException;
	List<META> bulkCreate(Map<KEY, KV<META>> kais, List<String> ids) throws IOException;
	List<InputStream> bulkRead(List<KEY> keys) throws IOException;
	List<META> bulkUpdate(Map<KEY, InputStream> mis)  throws IOException;
	List<META> bulkDelete(List<KEY> keys) throws IOException;
	List<META> bulkUndelete(List<KEY> keys) throws IOException;
	List<META> bulkRestore(List<KEY> keys) throws IOException;
	
	boolean exists(KEY key) throws IOException;
}
