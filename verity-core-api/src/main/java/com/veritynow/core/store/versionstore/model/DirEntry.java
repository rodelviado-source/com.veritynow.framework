package com.veritynow.core.store.versionstore.model;

import java.time.Instant;

public record DirEntry (
    Long id,
    String name,
    Inode parent,
    Inode child,
    Instant createdAt
  )  {

	public DirEntry(Inode parent, String name, Inode child) {
		this(null, name, parent, child, null);
	}
    
}
