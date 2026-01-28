package com.veritynow.core.store.meta;

import com.veritynow.core.store.base.PathEvent;

public record VersionMeta(
		String hashAlgorithm,
		String hash,
        String name,
        String mimeType,
        long   size,
        
        //What path was affected (absolute, namespace-qualified)
        String path,

        // When (epoch millis)
        Long timestamp,
        
        //How Operation (domain-level verb; not necessarily an HTTP verb)
        String operation,
        
        //Who (required, but can be anonymous)
        String principal,

        // Correlation (required)
        String correlationId,
        
        // Transaction (required)
        String workflowId,
        
        //Context Name
        String contextName,
        
        String transactionId,
        
        String transactionResult
) {
	
		
	    public VersionMeta(BlobMeta bm, PathEvent pe) {
	    	this(bm.hashAlgorithm(),  bm.hash(), bm.name(), bm.mimeType(), bm.size(),
	    			pe.path(), pe.timestamp(),pe.operation(),pe.principal(),pe.correlationId(), pe.workflowId(), pe.contextName(), pe.transactionId(), pe.transactionResult());
	    }
	
	    public BlobMeta blobMeta() {
	    	return new BlobMeta(hashAlgorithm, hash, name, mimeType, size) ;
	    }
	    
	    public PathEvent pathEvent() {
	    	return new PathEvent(path, timestamp, operation,  principal, correlationId, workflowId, contextName, transactionId, transactionResult) ;
	    }
}
