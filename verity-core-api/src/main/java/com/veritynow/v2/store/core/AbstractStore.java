package com.veritynow.v2.store.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.veritynow.v2.store.HashingService;
import com.veritynow.v2.store.Store;
import com.veritynow.v2.store.StoreCapabilities;



public abstract class AbstractStore<KEY, META> implements Store<KEY, META> {

			
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
	
	public AbstractStore(StoreCapabilities ...ops ) {
		this.capabilities = Set.of(ops);
		try {
			this.hs = new DefaultHashingService("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No such algorithm : SHA-1",e);
		}
	}
	
	public AbstractStore() {
		this.capabilities = Set.of(
				StoreCapabilities.CREATE, 
				StoreCapabilities.READ, 
				StoreCapabilities.UPDATE, 
				StoreCapabilities.DELETE, 
				StoreCapabilities.UNDELETE,
				StoreCapabilities.RESTORE);
		try {
			this.hs = new DefaultHashingService("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No such algorithm : SHA-1");
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
	public List<META> bulkCreate(Map<KEY, KV<META>> mis) throws IOException {
		List<META> out = new ArrayList<META>();
		mis.forEach((m, kv) -> {
			Optional<META> opt;
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
	public List<META> bulkCreate(Map<KEY, KV<META>> mis, List<String> ids) throws IOException {
		List<META> out = new ArrayList<META>();
		mis.forEach((m, kv) -> {
			Optional<META> opt;
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
	public List<META> bulkUpdate(Map<KEY, InputStream> mis) throws IOException {
		List<META> out = new ArrayList<META>();
		mis.forEach((m, i) -> {
			
			META cm = null;
			try {
				Optional<META> cmo = update(m, i);
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
	public List<META> bulkDelete(List<KEY> keys) throws IOException {
		List<META> out = new ArrayList<META>();
		keys.forEach((m) -> {
			
			META cm = null;
			try {
				Optional<META> cmo = delete(m);
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
	public List<META> bulkUndelete(List<KEY> keys) throws IOException {
		List<META> out = new ArrayList<META>();
		keys.forEach((m) -> {
			
			META cm = null;
			try {
				Optional<META> cmo = undelete(m);
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
	public List<META> bulkRestore(List<KEY> keys) throws IOException {
		List<META> out = new ArrayList<META>();
		keys.forEach((m) -> {
			
			META cm = null;
			try {
				Optional<META> cmo = restore(m);
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
