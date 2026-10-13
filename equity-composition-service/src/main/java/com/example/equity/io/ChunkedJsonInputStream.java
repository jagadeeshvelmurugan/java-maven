package com.example.equity.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ChunkedJsonInputStream extends InputStream {

    private final List<byte[]> chunks;
    private int chunkIndex = 0;
    private int pos = 0;

    public ChunkedJsonInputStream(List<byte[]> chunks) { this.chunks = chunks; }

    @Override public int read() throws IOException {
        if (!ensureBuffer()) return -1;
        return chunks.get(chunkIndex)[pos++] & 0xFF;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;
        if (!ensureBuffer()) return -1;
        byte[] cur = chunks.get(chunkIndex);
        int toCopy = Math.min(cur.length - pos, len);
        System.arraycopy(cur, pos, b, off, toCopy);
        pos += toCopy;
        return toCopy;
    }

    private boolean ensureBuffer() {
        while (chunkIndex < chunks.size() && pos >= chunks.get(chunkIndex).length) {
            chunkIndex++; pos = 0;
        }
        return chunkIndex < chunks.size();
    }
}
