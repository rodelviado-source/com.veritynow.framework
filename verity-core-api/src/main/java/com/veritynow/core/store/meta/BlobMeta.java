package com.veritynow.core.store.meta;

public record BlobMeta(
		String hashAlgorithm,
        String hash,
        String name,
        String mimeType,
        long   size
) {
	
	public BlobMeta(String name,  String mimeType,  long   size) {
		this(null, null, name, mimeType, size);
	}
	
	public static BlobMeta empty() {
		return new BlobMeta(null,null,null,null,0l);
	}
}
