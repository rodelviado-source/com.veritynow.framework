package com.veritynow.core.store.base;
public record PK(String path, String hash) { 
	
	public static PK path(String path) {
		return new PK(path, null);
	}
	
	public static PK hash(String hash) {
		return new PK(null, hash);
	}
	
} 