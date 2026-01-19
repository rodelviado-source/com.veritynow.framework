package com.veritynow.v2.store.core.jpa;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Store-internal path key codec.
 *
 * Produces the same ltree textual format as vn_path_to_scope_key(TEXT):
 *   h + substr(md5(seg), 1, 16) joined by '.'
 *
 * Locking will later consume the resulting scopeKey directly and MUST NOT recompute hashes.
 */
final class PathKeyCodec {

    private PathKeyCodec() {}

    static String segLabelMd5_16(String seg) {
        String md5 = md5Hex(seg);
        // Match Postgres substr(md5(seg), 1, 16)
        return "h" + md5.substring(0, 16);
    }

    static String appendSegLabel(String parentScopeKey, String segLabel) {
        if (parentScopeKey == null || parentScopeKey.isBlank()) return segLabel;
        return parentScopeKey + "." + segLabel;
    }

    static String scopeKeyFromSegments(List<String> segments) {
        String out = "";
        for (String seg : segments) {
            if (seg == null || seg.isEmpty()) continue;
            String label = segLabelMd5_16(seg);
            out = appendSegLabel(out, label);
        }
        return out;
    }

    private static String md5Hex(String s) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed in the JDK.
            throw new IllegalStateException("MD5 not available", e);
        }
        byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
        char[] out = new char[digest.length * 2];
        final char[] HEX = "0123456789abcdef".toCharArray();
        for (int i = 0; i < digest.length; i++) {
            int v = digest[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
