package com.example.equity.service;

import com.example.equity.io.ChecksumUtil;
import com.example.equity.model.CompositionRecord;
import com.example.equity.model.Context;
import com.example.equity.model.MarketData;
import com.example.equity.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EquityCompositionPayloadBuilder {

    private static final Logger log = LoggerFactory.getLogger(EquityCompositionPayloadBuilder.class);

    public static final String SCHEMA_VERSION  = "1.0";
    public static final int    EXPECTED_CHUNKS = 2;

    private final JsonFactory jsonFactory;
    private final int         chunkSizeBytes;

    public EquityCompositionPayloadBuilder(
            JsonFactory jsonFactory,
            @Value("${equity.chunk.size-bytes:1048576}") int chunkSizeBytes) {
        this.jsonFactory = jsonFactory;
        this.chunkSizeBytes = chunkSizeBytes;
    }

    public record PayloadBuild(byte[] document, int metaLength, int chunk0Length,
                               int recordCount, int rowCount, String contentChecksum) {}

    public PayloadBuild build(Context ctx) throws IOException {
        long t0 = System.currentTimeMillis();

        ContentBuild content = buildContent(ctx.getRecords());
        log.info("Content built: {} bytes, {} records, {} rows, sha256={}",
                content.bytes.length, content.recordCount, content.rowCount,
                content.checksum.substring(0, 16));

        Meta meta = buildMeta(ctx, content);
        byte[] metaBytes = writeMeta(meta);

        byte[] document = assemble(metaBytes, content.bytes);

        int headerLen = 8 + metaBytes.length + 11;
        int contentFirstHalf = content.bytes.length / 2;
        int chunk0Length = headerLen + contentFirstHalf;

        log.info("Document assembled: {} bytes (meta={}, chunk0={}) in {} ms",
                document.length, metaBytes.length, chunk0Length,
                System.currentTimeMillis() - t0);

        return new PayloadBuild(document, metaBytes.length, chunk0Length,
                content.recordCount, content.rowCount, content.checksum);
    }

    /*private ContentBuild buildContent(List<CompositionRecord> records) throws IOException {
        if (records == null) records = List.of();

        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
        int recordCount = 0;
        int rowCount = 0;

        try (JsonGenerator gen = jsonFactory.createGenerator(
                ObjectWriteContext.empty(), out, JsonEncoding.UTF8)) {
            gen.writeStartArray();

            for (CompositionRecord r : records) {
                recordCount++;
                List<MarketData> mds = r.getOutput();
                if (mds == null) continue;
                for (MarketData md : mds) {
                    LinkedHashMap<String, Object> dyn = md.getDynamicValues();
                    if (dyn == null || dyn.isEmpty()) continue;
                    writeDynamicMap(gen, dyn);
                    rowCount++;
                }
            }

            gen.writeEndArray();
        }

        byte[] bytes = out.toByteArray();
        return new ContentBuild(bytes, recordCount, rowCount, ChecksumUtil.sha256Hex(bytes));
    }*/

    private ContentBuild buildContent(List<CompositionRecord> records) throws IOException {
        if (records == null) records = List.of();

        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
        int recordCount = 0;
        int rowCount    = 0;

        // Content array opens on its own line
        out.write('[');
        out.write('\n');

        boolean first = true;
        ByteArrayOutputStream rowBuf = new ByteArrayOutputStream(256);

        for (CompositionRecord r : records) {
            recordCount++;
            List<MarketData> mds = r.getOutput();
            if (mds == null) continue;

            for (MarketData md : mds) {
                LinkedHashMap<String, Object> dyn = md.getDynamicValues();
                if (dyn == null || dyn.isEmpty()) continue;

                if (!first) {
                    out.write(',');
                    out.write('\n');
                }

                // Serialize one dynamicValues map to its own buffer so the newline
                // separating array elements stays clean (no auto-comma interleaving).
                rowBuf.reset();
                try (JsonGenerator rowGen = jsonFactory.createGenerator(
                        ObjectWriteContext.empty(), rowBuf, JsonEncoding.UTF8)) {
                    writeDynamicMap(rowGen, dyn);
                }
                rowBuf.writeTo(out);

                first = false;
                rowCount++;
            }
        }

        if (rowCount > 0) {
            out.write('\n');
        }
        out.write(']');

        byte[] bytes = out.toByteArray();
        return new ContentBuild(bytes, recordCount, rowCount, ChecksumUtil.sha256Hex(bytes));
    }

    private record ContentBuild(byte[] bytes, int recordCount, int rowCount, String checksum) {}

    private void writeDynamicMap(JsonGenerator gen, Map<String, Object> map) throws IOException {
        gen.writeStartObject();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            writeDynamicPair(gen, e.getKey(), e.getValue());
        }
        gen.writeEndObject();
    }

    private void writeDynamicPair(JsonGenerator gen, String key, Object v) throws IOException {
        if (v == null) {
            gen.writeNullProperty(key);
        } else if (v instanceof String s) {
            gen.writeStringProperty(key, s);
        } else if (v instanceof Boolean b) {
            gen.writeBooleanProperty(key, b);
        } else if (v instanceof Number n) {
            gen.writeName(key);
            writeNumber(gen, n);
        } else if (v instanceof java.time.temporal.Temporal t) {
            gen.writeStringProperty(key, t.toString());
        } else if (v instanceof List<?> list) {
            gen.writeArrayPropertyStart(key);
            for (Object item : list) writeDynamicValue(gen, item);
            gen.writeEndArray();
        } else if (v instanceof Map<?, ?> m) {
            gen.writeObjectPropertyStart(key);
            for (Map.Entry<?, ?> e : m.entrySet()) {
                writeDynamicPair(gen, String.valueOf(e.getKey()), e.getValue());
            }
            gen.writeEndObject();
        } else {
            gen.writeStringProperty(key, v.toString());
        }
    }

    private void writeDynamicValue(JsonGenerator gen, Object v) throws IOException {
        if (v == null) {
            gen.writeNull();
        } else if (v instanceof String s) {
            gen.writeString(s);
        } else if (v instanceof Boolean b) {
            gen.writeBoolean(b);
        } else if (v instanceof Number n) {
            writeNumber(gen, n);
        } else if (v instanceof java.time.temporal.Temporal t) {
            gen.writeString(t.toString());
        } else if (v instanceof List<?> list) {
            gen.writeStartArray();
            for (Object item : list) writeDynamicValue(gen, item);
            gen.writeEndArray();
        } else if (v instanceof Map<?, ?> m) {
            gen.writeStartObject();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                writeDynamicPair(gen, String.valueOf(e.getKey()), e.getValue());
            }
            gen.writeEndObject();
        } else {
            gen.writeString(v.toString());
        }
    }

    private void writeNumber(JsonGenerator gen, Number n) throws IOException {
        if (n instanceof Integer || n instanceof Short || n instanceof Byte) {
            gen.writeNumber(n.intValue());
        } else if (n instanceof Long) {
            gen.writeNumber(n.longValue());
        } else if (n instanceof java.math.BigInteger bi) {
            gen.writeNumber(bi);
        } else if (n instanceof java.math.BigDecimal bd) {
            gen.writeNumber(bd);
        } else if (n instanceof Double) {
            gen.writeNumber(n.doubleValue());
        } else if (n instanceof Float) {
            gen.writeNumber(n.floatValue());
        } else {
            gen.writeNumber(n.doubleValue());
        }
    }

    private Meta buildMeta(Context ctx, ContentBuild content) {
        Meta m = new Meta();
        m.setContextId(ctx.getContextId());
        m.setPortfolioId(ctx.getPortfolioId());
        m.setAsOfDate(ctx.getAsOfDate());
        m.setGeneratedAt(java.time.Instant.now());
        m.setSchemaVersion(SCHEMA_VERSION);
        m.setDataSource(ctx.getDataSource());
        m.setSourceSystem(ctx.getSourceSystem());

        m.setRecordCount(content.recordCount);
        m.setRowCount(content.rowCount);
        m.setContentBytes(content.bytes.length);
        m.setContentChecksum(content.checksum);

        m.setChunkSize(chunkSizeBytes);
        m.setExpectedChunks(EXPECTED_CHUNKS);
        m.setExtras(ctx.getExtras());
        return m;
    }

    private byte[] writeMeta(Meta m) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        try (JsonGenerator gen = jsonFactory.createGenerator(
                ObjectWriteContext.empty(), out, JsonEncoding.UTF8)) {
            gen.writeStartObject();

            if (m.getContextId()     != null) gen.writeStringProperty("contextId",     m.getContextId());
            if (m.getPortfolioId()   != null) gen.writeStringProperty("portfolioId",   m.getPortfolioId());
            if (m.getAsOfDate()      != null) gen.writeStringProperty("asOfDate",      m.getAsOfDate().toString());
            if (m.getGeneratedAt()   != null) gen.writeStringProperty("generatedAt",   m.getGeneratedAt().toString());
            if (m.getSchemaVersion() != null) gen.writeStringProperty("schemaVersion", m.getSchemaVersion());
            if (m.getDataSource()    != null) gen.writeStringProperty("dataSource",    m.getDataSource());
            if (m.getSourceSystem()  != null) gen.writeStringProperty("sourceSystem",  m.getSourceSystem());

            gen.writeName("recordCount");
            gen.writeNumber(m.getRecordCount());

            gen.writeName("rowCount");
            gen.writeNumber(m.getRowCount());

            gen.writeName("contentBytes");
            gen.writeNumber(m.getContentBytes());

            if (m.getContentChecksum() != null) {
                gen.writeStringProperty("contentChecksum", m.getContentChecksum());
            }

            gen.writeName("chunkSize");
            gen.writeNumber(m.getChunkSize());

            gen.writeName("expectedChunks");
            gen.writeNumber(m.getExpectedChunks());

            Map<String, Object> extras = m.getExtras();
            if (extras != null && !extras.isEmpty()) {
                gen.writeName("extras");
                writeDynamicMap(gen, extras);
            }

            gen.writeEndObject();
        }
        return out.toByteArray();
    }

    private byte[] assemble(byte[] metaBytes, byte[] contentBytes) {
        byte[] prefix = "{\"meta\":".getBytes(StandardCharsets.UTF_8);
        byte[] middle = ",\"content\":".getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "}".getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[prefix.length + metaBytes.length
                + middle.length + contentBytes.length + suffix.length];

        int p = 0;
        System.arraycopy(prefix,       0, result, p, prefix.length);       p += prefix.length;
        System.arraycopy(metaBytes,    0, result, p, metaBytes.length);    p += metaBytes.length;
        System.arraycopy(middle,       0, result, p, middle.length);       p += middle.length;
        System.arraycopy(contentBytes, 0, result, p, contentBytes.length); p += contentBytes.length;
        System.arraycopy(suffix,       0, result, p, suffix.length);
        return result;
    }
}
