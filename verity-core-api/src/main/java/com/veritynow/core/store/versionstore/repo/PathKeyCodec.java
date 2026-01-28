package com.veritynow.core.store.versionstore.repo;

import java.util.ArrayList;
import java.util.List;

import com.veritynow.core.store.versionstore.PathUtils;

import net.openhft.hashing.LongHashFunction;

/**
 * Path → ltree key using XXH3 (non-cryptographic, fast).
 */
public final class PathKeyCodec {

    // 64-bit hash → 16 hex chars
    private static final int HEX_LEN = 16;
    private static final LongHashFunction HASH = LongHashFunction.xx3();
    
    public static final String ROOT_LABEL = label("/");

    private PathKeyCodec() {}

    public static String toLTree(String path) {
        String p = PathUtils.normalizePath(path);

        if ("/".equals(p)) {
            return ROOT_LABEL;
        }

        List<String> segs = splitSegments(p);

        StringBuilder sb = new StringBuilder(ROOT_LABEL);
        for (String seg : segs) {
            sb.append('.').append(label(seg));
        }
        return sb.toString();
    }
    
    
    public static String xxh3Encode(String seg) {
        
        long h = HASH.hashChars(seg);
        String hex = Long.toHexString(h);
        // left-pad or truncate to fixed 16 chars (recommended)
        if (hex.length() < HEX_LEN) {
             hex = "0".repeat(HEX_LEN - hex.length()) + hex;
        } else if (hex.length() > HEX_LEN) {
            hex = hex.substring(0, HEX_LEN);
        }

        return hex;
    }

    private static List<String> splitSegments(String normalizedPath) {
        String[] parts = normalizedPath.split("/");
        List<String> out = new ArrayList<>(parts.length);
        for (String s : parts) {
            if (s == null || s.isBlank()) continue;
            out.add(s);
        }
        return out;
    }
    
    static String appendSegLabel(String parentScopeKey, String segLabel) {
        if (parentScopeKey == null || parentScopeKey.isBlank()) return segLabel;
        return parentScopeKey + "." + segLabel;
    }
    
    public static String label(String seg) { return "h" + xxh3Encode(seg); }
}
