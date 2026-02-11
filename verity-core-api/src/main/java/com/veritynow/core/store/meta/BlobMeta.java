package com.veritynow.core.store.meta;

public record BlobMeta(
		String hashAlgorithm,
        String hash,
        String name,
        String mimeType,
        long   size
) {
			
	public BlobMeta(String name,  String mimeType) {
		this(null, null, name, mimeType, 0);
	}
	
}
