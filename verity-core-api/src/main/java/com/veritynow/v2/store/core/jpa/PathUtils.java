package com.veritynow.v2.store.core.jpa;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {
	public static String normalizePath(String p) {
        if (p == null) return null;
        // matches FS normalizePath: ensure leading '/' :contentReference[oaicite:18]{index=18}
        return p.startsWith("/") ? p : ("/" + p);
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
