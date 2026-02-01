package com.veritynow.core.store.versionstore;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {
	
	
	public static String normalizeAndApplyNamespace(String path, String namespace) {
		String ns = normalizeNamespace(namespace);
		String p = normalizePath(path);

		if (ns.isEmpty())
			return p;
		if (p.equals("/"))
			return ns;
		return ns + p;
	}
	
	public static String normalizeNamespace(String namespace) {
		if (namespace == null)
			return "";

		String ns = namespace.trim();
		if (ns.isEmpty() || ns.equals("/"))
			return "";

		// remove leading slashes
		ns = ns.replaceAll("^/+", "");
		// remove trailing slashes
		ns = ns.replaceAll("/+$", "");

		return ns.isEmpty() ? "" : ("/" + ns);
	}
	
	public static String removeNamespace(String internalPath, String namespace) {
		String ns = normalizeNamespace(namespace);
		String p = normalizePath(internalPath);

		if (ns.isEmpty())
			return p;
		if (p.equals(ns))
			return "/";
		if (p.startsWith(ns + "/"))
			return p.substring(ns.length());
		return p;
	}
	
	
	public static String normalizePath(String path) {
        if (path == null) throw new IllegalArgumentException("path");
        String p = path.trim();
        if (p.isEmpty()) return "/";
        // Replace backslashes
        p = p.replace('\\', '/');
        // Ensure leading slash
        if (!p.startsWith("/")) p = "/" + p;
        // Collapse multiple slashes
        p = p.replaceAll("/+", "/");
        // Remove trailing slash unless root
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.isEmpty() ? "/" : p;
    }

    public static String trimLeadingSlash(String p) {
        return p != null && p.startsWith("/") ? p.substring(1) : p;
    }

    public static String trimEndingSlash(String p) {
        return p != null && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    public static String lastSegment(String path) {
        int i = path.lastIndexOf('/');
        return (i >= 0) ? path.substring(i + 1) : path;
    }

    public  static List<String> splitSegments(String nodePath) {
        String p = trimLeadingSlash(normalizePath(nodePath));
        if (p.isBlank()) return List.of();
        return Arrays.stream(p.split("/"))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

}
