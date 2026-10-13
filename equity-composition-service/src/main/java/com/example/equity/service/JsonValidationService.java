package com.example.equity.service;

import com.example.equity.io.ChecksumUtil;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class JsonValidationService {

    private static final ObjectReadContext READ_CONTEXT = ObjectReadContext.empty();

    private final JsonFactory jsonFactory;
    private final JsonMapper  jsonMapper;

    public JsonValidationService(JsonFactory jsonFactory, JsonMapper jsonMapper) {
        this.jsonFactory = jsonFactory;
        this.jsonMapper  = jsonMapper;
    }

    public record ValidationResult(boolean ok, String message,
                                   long bytes, long tokens, String checksum) {}

    public record ShapeResult(boolean ok, String message, int rowCount) {}

    public ValidationResult validate(byte[] json) {
        if (json == null || json.length == 0) {
            return new ValidationResult(false, "Empty payload", 0, 0, null);
        }
        try (JsonParser parser = jsonFactory.createParser(READ_CONTEXT, new ByteArrayInputStream(json))) {
            long tokens = drain(parser);
            return new ValidationResult(true, "OK", json.length, tokens,
                    ChecksumUtil.sha256Hex(json));
        } catch (Exception e) {
            return new ValidationResult(false,
                    "JSON parse error: " + e.getMessage(), json.length, 0, null);
        }
    }

    public ValidationResult validate(InputStream in) {
        try (JsonParser parser = jsonFactory.createParser(READ_CONTEXT, in)) {
            long tokens = drain(parser);
            return new ValidationResult(true, "OK", -1, tokens, null);
        } catch (Exception e) {
            return new ValidationResult(false,
                    "JSON parse error: " + e.getMessage(), -1, 0, null);
        }
    }

    public ValidationResult validateChunks(List<byte[]> chunks, List<String> expectedChecksums) {
        if (chunks == null || chunks.isEmpty()) {
            return new ValidationResult(false, "No chunks to validate", 0, 0, null);
        }
        if (chunks.size() != expectedChecksums.size()) {
            return new ValidationResult(false,
                    "Chunk count mismatch: data=" + chunks.size() +
                    " checksums=" + expectedChecksums.size(), 0, 0, null);
        }
        long total = 0;
        for (int i = 0; i < chunks.size(); i++) {
            byte[] c = chunks.get(i);
            total += c.length;
            String actual = ChecksumUtil.sha256Hex(c);
            String expected = expectedChecksums.get(i);
            if (expected == null || !expected.equalsIgnoreCase(actual)) {
                return new ValidationResult(false,
                        "Chunk " + i + " checksum mismatch: expected=" + expected +
                        " actual=" + actual, total, 0, null);
            }
        }
        try (JsonParser parser = jsonFactory.createParser(READ_CONTEXT, new ChunkedBytesInputStream(chunks))) {
            long tokens = drain(parser);
            String docChecksum = ChecksumUtil.sha256OfStrings(
                    expectedChecksums.toArray(new String[0]));
            return new ValidationResult(true, "OK", total, tokens, docChecksum);
        } catch (Exception e) {
            return new ValidationResult(false,
                    "Reassembled JSON parse error: " + e.getMessage(), total, 0, null);
        }
    }

    public ShapeResult checkShape(byte[] json) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return new ShapeResult(false, "root not an object", 0);
            }
            if (!root.path("meta").isObject()) {
                return new ShapeResult(false, "no meta object", 0);
            }
            JsonNode content = root.path("content");
            if (!content.isArray()) {
                return new ShapeResult(false, "no content array", 0);
            }
            int rows = 0;
            for (JsonNode row : content) {
                if (!row.isObject()) {
                    return new ShapeResult(false, "content[" + rows + "] not object", rows);
                }
                rows++;
            }
            return new ShapeResult(true, "OK", rows);
        } catch (Exception e) {
            return new ShapeResult(false, e.getMessage(), 0);
        }
    }

    private long drain(JsonParser parser) throws JacksonException {
        long tokens = 0;
        boolean firstTokenSeen = false;
        JsonToken t;
        while ((t = parser.nextToken()) != null) {
            tokens++;
            if (!firstTokenSeen) {
                firstTokenSeen = true;
                parser.skipChildren();
            }
        }
        if (!firstTokenSeen) throw new IllegalStateException("No JSON content");
        return tokens;
    }

    private static final class ChunkedBytesInputStream extends InputStream {
        private final List<byte[]> chunks;
        private int idx = 0, pos = 0;
        ChunkedBytesInputStream(List<byte[]> chunks) { this.chunks = chunks; }
        @Override public int read() {
            if (!ensure()) return -1;
            return chunks.get(idx)[pos++] & 0xFF;
        }
        @Override public int read(byte[] b, int off, int len) {
            if (len == 0) return 0;
            if (!ensure()) return -1;
            byte[] cur = chunks.get(idx);
            int n = Math.min(cur.length - pos, len);
            System.arraycopy(cur, pos, b, off, n);
            pos += n;
            return n;
        }
        private boolean ensure() {
            while (idx < chunks.size() && pos >= chunks.get(idx).length) { idx++; pos = 0; }
            return idx < chunks.size();
        }
    }
}
