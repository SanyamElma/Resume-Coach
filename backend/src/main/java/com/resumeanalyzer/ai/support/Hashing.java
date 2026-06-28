package com.resumeanalyzer.ai.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Small SHA-256 helper used to build stable cache keys and content hashes. */
public final class Hashing {

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    /** Joins parts with a separator that cannot appear in the parts, then hashes. */
    public static String key(String... parts) {
        return sha256Hex(String.join("", parts));
    }
}
