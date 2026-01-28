package com.veritynow.core.store.versionstore.model;

import java.time.Instant;


public record Inode(Long id, Instant createdAt,String scopeKey )

{

	public Inode(String childScopeKey) {
		this(null, null, childScopeKey);
	} 

    
}
