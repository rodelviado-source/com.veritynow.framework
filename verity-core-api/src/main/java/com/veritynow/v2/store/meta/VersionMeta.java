package com.veritynow.v2.store.meta;

import com.veritynow.v2.txn.core.PathEvent;

public record VersionMeta(
		String hash,
        String name,
        String mimeType,
        long   size,
        
        //What path was affected (absolute, namespace-qualified)
        String path,

        // When (epoch millis)
        long timestamp,
        
        //How Operation (domain-level verb; not necessarily an HTTP verb)
        String operation,
        
        //Who (required, but can be anonymous)
        String principal,

        // Correlation (required)
        String correlationId,
        
        // Transaction (required)
        String transactionId,
        
        //Context Name
        String contextName
) {
	
	    public VersionMeta(BlobMeta bm, PathEvent pe) {
	    	this(bm.hash(), bm.name(), bm.mimeType(), bm.size(),
	    			pe.path(), pe.timestamp(),pe.operation(),pe.principal(),pe.correlationId(), pe.transactionId(), pe.contextName());	
	    }
	
	    public BlobMeta blobMeta() {
	    	return new BlobMeta(hash, name, mimeType, size) ;
	    }
	    
	    public PathEvent pathEvent() {
	    	return new PathEvent(path, timestamp, operation,  principal, correlationId, transactionId, contextName) ;
	    }
}
