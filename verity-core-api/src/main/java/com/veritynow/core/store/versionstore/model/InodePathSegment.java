package com.veritynow.core.store.versionstore.model;

import java.time.Instant;


public record InodePathSegment ( 
		Long id,  
		Inode inode, 
		int ord, 
		DirEntry dirEntry, 
		Instant createdAt
)

{

	public InodePathSegment(Inode inode, int ord, DirEntry dirEntry) {
		this(null, inode, ord, dirEntry, null);
	}
}