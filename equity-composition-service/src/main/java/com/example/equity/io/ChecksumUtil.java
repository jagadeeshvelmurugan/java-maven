package com.example.equity.io;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ChecksumUtil {

    private ChecksumUtil() {}

    public static String sha256Hex(byte[] data) {
        return sha256Hex(data, 0, data.length);
    }

    public static String sha256Hex(byte[] data, int off, int len) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data, off, len);
            return toHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String sha256OfStrings(String... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String s : parts) {
                    if (s != null) md.update(s.getBytes(StandardCharsets.US_ASCII));
                }
            }
            return toHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2]     = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}
