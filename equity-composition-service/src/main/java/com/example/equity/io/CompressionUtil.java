package com.example.equity.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CompressionUtil {

    public static final String ALGO_GZIP = "GZIP";

    private CompressionUtil() {}

    public static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 4));
        try (GZIPOutputStream gz = new GZIPOutputStream(out, 8192)) {
            gz.write(data);
        }
        byte[] compressed = out.toByteArray();
        return compressed.length < data.length ? compressed : null;
    }

    public static byte[] gunzip(byte[] compressed) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            gz.transferTo(out);
        }
        return out.toByteArray();
    }
}
