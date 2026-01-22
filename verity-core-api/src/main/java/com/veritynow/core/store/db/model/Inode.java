package com.veritynow.core.store.db.model;

import java.time.Instant;


public record Inode(Long id, Instant createdAt,String scopeKey )

{

	public Inode(Instant now, String childScopeKey) {
		this(null, now, childScopeKey);
	} 

    
}
