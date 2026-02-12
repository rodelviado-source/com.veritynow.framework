package com.veritynow.core.store.base;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.veritynow.core.store.HashingService;
import com.veritynow.core.store.Store;
import com.veritynow.core.store.StoreCapabilities;



public abstract class AbstractStore<KEY, I, O> implements Store<KEY, I, O> {

			
	private final Set<StoreCapabilities> capabilities;
	HashingService hs;
	
	public AbstractStore(HashingService hs, StoreCapabilities ...ops ) {
		this.capabilities = Set.of(ops);
		
		if (hs == null) {
			try {
				this.hs = new DefaultHashingService("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("No such algorithm : SHA-1",e);
			}
		} else {
			this.hs = hs;
		}
	}
	
		
	
	public boolean canCreate() { return capabilities.contains(StoreCapabilities.CREATE) ;}
	public boolean canRead() { return capabilities.contains(StoreCapabilities.READ) ;}
	public boolean canUpdate() { return capabilities.contains(StoreCapabilities.UPDATE) ;}
	public boolean canDelete() { return capabilities.contains(StoreCapabilities.DELETE) ;}
	public boolean canUnDelete() { return capabilities.contains(StoreCapabilities.UNDELETE) ;}
	public boolean canRestore() { return capabilities.contains(StoreCapabilities.RESTORE) ;}
	
	public boolean canBulkCreate() { return capabilities.contains(StoreCapabilities.BULK_CREATE) ;}
	public boolean canBulkRead() { return capabilities.contains(StoreCapabilities.BULK_READ) ;}
	public boolean canBulkUpdate() { return capabilities.contains(StoreCapabilities.BULK_UPDATE) ;}
	public boolean canBulkDelete() { return capabilities.contains(StoreCapabilities.BULK_DELETE) ;}
	public boolean canBulkUndelete() { return capabilities.contains(StoreCapabilities.BULK_UNDELETE) ;}
	public boolean canBulkRestore() { return capabilities.contains(StoreCapabilities.BULK_RESTORE) ;}
	
	Set<StoreCapabilities> capabilities() {return capabilities;};
	
	public HashingService getHashingService() {
		return hs;
	}

	/** 
	 * 
	 * Default naive impl of bulk operations, 
	 * Override for optimized versions of your store 
	 * 
	 * **/
	
	@Override
	public List<O> bulkCreate(Map<KEY, KV<I>> mis) throws IOException {
		List<O> out = new ArrayList<>();
		mis.forEach((m, kv) -> {
			Optional<O> opt;
			try {
				opt = create(m, kv.meta(), kv.inputStream());
				if (opt.isPresent()) {
					out.add(opt.get());
				}
			} catch (IOException e) {
				//
			}
			
		});
		
		return out;
	}
	
	@Override
	public List<O> bulkCreate(Map<KEY, KV<I>> mis, List<String> ids) throws IOException {
		List<O> out = new ArrayList<>();
		mis.forEach((m, kv) -> {
			Optional<O> opt;
			int idx = 0;
			try {
				opt = create(m, kv.meta(), kv.inputStream(), ids.get(idx++));
				if (opt.isPresent()) {
					out.add(opt.get());
				}
			} catch (IOException e) {
				//
			}
			
		});
		
		return out;
	}

	@Override
	public List<InputStream> bulkRead(List<KEY> keys) throws IOException {
		List<InputStream> out = new ArrayList<InputStream>();
		keys.forEach((m) -> {
			
			InputStream cm = null;
			try {
				Optional<InputStream> cmo = read(m);
				if (cmo.isPresent()) cm = cmo.get();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cm != null) {
				out.add(cm);
			}	
			} );
		
		return out;
	}

	@Override
	public List<O> bulkUpdate(Map<KEY, InputStream> mis) throws IOException {
		List<O> out = new ArrayList<>();
		mis.forEach((m, i) -> {
			
			O cm = null;
			try {
				Optional<O> cmo = update(m, i);
				if (cmo.isPresent()) cm = cmo.get();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cm != null) {
				out.add(cm);
			}	
			} );
		
		return out;
	}

	@Override
	public List<O> bulkDelete(List<KEY> keys) throws IOException {
		List<O> out = new ArrayList<>();
		keys.forEach((m) -> {
			
			O cm = null;
			try {
				Optional<O> cmo = delete(m);
				if (cmo.isPresent()) cm = cmo.get();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cm != null) {
				out.add(cm);
			}	
			} );
		
		return out;
	}

	@Override
	public List<O> bulkUndelete(List<KEY> keys) throws IOException {
		List<O> out = new ArrayList<>();
		keys.forEach((m) -> {
			
			O cm = null;
			try {
				Optional<O> cmo = undelete(m);
				if (cmo.isPresent()) cm = cmo.get();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cm != null) {
				out.add(cm);
			}	
			} );
		
		return out;
	}

	@Override
	public List<O> bulkRestore(List<KEY> keys) throws IOException {
		List<O> out = new ArrayList<>();
		keys.forEach((m) -> {
			
			O cm = null;
			try {
				Optional<O> cmo = restore(m);
				if (cmo.isPresent()) cm = cmo.get();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (cm != null) {
				out.add(cm);
			}	
			} );
		
		return out;
	}
		

	
}
