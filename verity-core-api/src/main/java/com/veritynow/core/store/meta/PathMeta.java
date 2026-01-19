package com.veritynow.core.store.meta;

import java.util.List;

public record PathMeta(
    String path,
    String type,
    List<String> children,
    List<VersionMeta> versions
) {
	
	public  PathMeta {
		if (!getType(children, versions).equals(type)) 
			  throw new IllegalArgumentException("Type is inconsistent with the path state " + type);
	}
	
	public PathMeta(String path,   List<String> children, List<VersionMeta> versions) {
		this(path, getType(children, versions), children, versions);
	}
	
    
    
    public static String getType(List<String> children, List<VersionMeta> versions) {
    	if (children.isEmpty() && versions.isEmpty()) {
        	return "Empty";
        }
        if (children.isEmpty() && !versions.isEmpty()) {
        	return "Leaf";
        }
        if (!children.isEmpty() && versions.isEmpty()) {
        	return "Tree";
        }
        if (!children.isEmpty() && !versions.isEmpty()) {
        	return "Leaf|Tree"; 
        }
        return "Unknown";
    }
    
}
