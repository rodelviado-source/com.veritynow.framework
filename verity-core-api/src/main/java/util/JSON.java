package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JSON {
	
	public static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            //.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            //.configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false)
            .build();
	/**
     * Compute a SHA-256 hash over a JSON-serializable object, using canonical JSON.
     * (You can hook this into higher-level code; not used by JPA directly.)
     */
    public static String computeHash(Object o) {
        try {
            // serialize to canonical JSON
            String s = MAPPER.writeValueAsString(o);
            // parse into TreeMap to enforce key ordering, then re-serialize
            var map = MAPPER.readValue(s, TreeMap.class);
            String canonical = MAPPER.writeValueAsString(map);
            return computeHash(canonical.getBytes());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to compute JSON hash", e);
        }
    }

    /**
     * Canonicalizes a JSON string: parses it into a TreeMap, then re-serializes
     * with keys sorted. Returns null if input is null.
     */
    public static String canonicalizeOrNull(String json) {
        if (json == null) {
            return null;
        }
        try {
            var map = MAPPER.readValue(json, TreeMap.class);
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to canonicalize JSON meta", e);
        }
    }

    /**
     * Low-level content hash (SHA-256). Works for arbitrary bytes.
     */
    public static String computeHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);

            StringBuilder hexString = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
