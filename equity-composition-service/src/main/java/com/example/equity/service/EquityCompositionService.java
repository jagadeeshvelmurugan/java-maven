package com.example.equity.service;

import com.example.equity.config.CompressionProperties;
import com.example.equity.dao.EquityCompositionChunkDao;
import com.example.equity.dao.EquityCompositionChunkDao.ChunkWithData;
import com.example.equity.dao.EquityCompositionDao;
import com.example.equity.domain.EquityComposition;
import com.example.equity.domain.EquityCompositionChunk;
import com.example.equity.exception.ResourceNotFoundException;
import com.example.equity.io.ChecksumUtil;
import com.example.equity.io.ChunkedJsonInputStream;
import com.example.equity.io.CompressionUtil;
import com.example.equity.model.Context;
import com.example.equity.model.Meta;
import com.example.equity.service.EquityCompositionPayloadBuilder.PayloadBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class EquityCompositionService {

    private static final Logger log = LoggerFactory.getLogger(EquityCompositionService.class);

    private static final int    NUM_CHUNKS       = 2;
    private static final String CHUNK_DONE       = "COMPLETED";
    private static final String STATUS_COMPLETED = CompositionStateManager.STATUS_COMPLETED;
    private static final String STATUS_WRITING   = CompositionStateManager.STATUS_WRITING;
    private static final String STATUS_VALIDATING= CompositionStateManager.STATUS_VALIDATING;

    private static final int PREFIX_LEN = "{\"meta\":".length();
    private static final int MIDDLE_LEN = ",\"content\":".length();

    private static final ObjectReadContext READ_CONTEXT = ObjectReadContext.empty();

    private final EquityCompositionDao            parentDao;
    private final EquityCompositionChunkDao       chunkDao;
    private final JsonMapper                      jsonMapper;
    private final JsonFactory                     jsonFactory;
    private final JsonValidationService           jsonValidator;
    private final CompositionStateManager         stateManager;
    private final EquityCompositionPayloadBuilder payloadBuilder;
    private final CompressionProperties           compression;

    public EquityCompositionService(EquityCompositionDao parentDao,
                                    EquityCompositionChunkDao chunkDao,
                                    JsonMapper jsonMapper,
                                    JsonFactory jsonFactory,
                                    JsonValidationService jsonValidator,
                                    CompositionStateManager stateManager,
                                    EquityCompositionPayloadBuilder payloadBuilder,
                                    CompressionProperties compression) {
        this.parentDao = parentDao;
        this.chunkDao = chunkDao;
        this.jsonMapper = jsonMapper;
        this.jsonFactory = jsonFactory;
        this.jsonValidator = jsonValidator;
        this.stateManager = stateManager;
        this.payloadBuilder = payloadBuilder;
        this.compression = compression;
    }

    public EquityComposition generateAndPersist(Context ctx) throws IOException {
        PayloadBuild build = payloadBuilder.build(ctx);

        JsonValidationService.ValidationResult pre = jsonValidator.validate(build.document());
        if (!pre.ok()) {
            throw new IllegalStateException("Pre-persist validation failed: " + pre.message());
        }
        log.info("Pre-persist validation OK: bytes={}, tokens={}, sha256={}",
                pre.bytes(), pre.tokens(), pre.checksum().substring(0, 16));

        JsonValidationService.ShapeResult shape = jsonValidator.checkShape(build.document());
        if (!shape.ok()) {
            throw new IllegalStateException("Shape check failed: " + shape.message());
        }
        if (shape.rowCount() != build.rowCount()) {
            throw new IllegalStateException("Row count mismatch: shape=" + shape.rowCount()
                    + " builder=" + build.rowCount());
        }

        return persist(build.document(), ctx.getPortfolioId(), ctx.getAsOfDate(),
                build.metaLength(), build.chunk0Length());
    }

    public EquityComposition persist(byte[] document,
                                     String portfolioIdHint,
                                     LocalDate asOfDateHint,
                                     int metaLength,
                                     int chunk0Length) {

        long id = parentDao.insert(EquityComposition.builder()
                .portfolioId(portfolioIdHint)
                .asOfDate(asOfDateHint)
                .status(CompositionStateManager.STATUS_STARTED)
                .totalChunks(NUM_CHUNKS)
                .totalBytes((long) document.length)
                .storedBytes(0L)
                .latest("N")
                .build());

        stateManager.transition(id, STATUS_WRITING);

        try {
            byte[] chunk0 = Arrays.copyOfRange(document, 0,            chunk0Length);
            byte[] chunk1 = Arrays.copyOfRange(document, chunk0Length, document.length);
            insertChunk(id, 0, chunk0);
            insertChunk(id, 1, chunk1);
            stateManager.recordMetaLength(id, metaLength);
        } catch (Exception e) {
            log.error("Write failed for composition {}", id, e);
            stateManager.markFailed(id, e);
            throw new RuntimeException("Write failed for composition " + id, e);
        }

        stateManager.transition(id, STATUS_VALIDATING);

        JsonValidationService.ValidationResult post = validateStored(id);
        if (!post.ok()) {
            stateManager.markFailed(id,
                    new IllegalStateException("Post-write validation failed: " + post.message()));
            throw new IllegalStateException("Post-write validation failed for composition "
                    + id + ": " + post.message());
        }

        stateManager.finalizeCompleted(id, post);

        EquityComposition saved = parentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Composition not found: " + id));
        log.info("Persisted+validated composition id={}, uncompressed={}, stored={}",
                id, document.length, saved.getStoredBytes());
        return saved;
    }

    private void insertChunk(long compositionId, int seq, byte[] raw) {
        byte[]  toStore    = raw;
        boolean compressed = false;
        String  algo       = null;
        int     storedSize = raw.length;

        if (compression.isEnabled() && raw.length >= compression.getThresholdBytes()) {
            try {
                byte[] gz = CompressionUtil.gzip(raw);
                if (gz != null) {
                    toStore    = gz;
                    compressed = true;
                    algo       = compression.getAlgorithm();
                    storedSize = gz.length;
                    log.debug("Chunk {} compressed: {} -> {} bytes", seq, raw.length, gz.length);
                }
            } catch (Exception e) {
                log.warn("GZIP failed for chunk {} of composition {}; storing uncompressed",
                        seq, compositionId, e);
            }
        }

        String checksum = ChecksumUtil.sha256Hex(raw);
        chunkDao.insert(compositionId, seq, storedSize, raw.length,
                toStore, CHUNK_DONE, checksum, compressed, algo);
    }

    public JsonValidationService.ValidationResult validateStored(long compositionId) {
        List<EquityCompositionChunk> meta = chunkDao.listMetadata(compositionId);
        if (meta.isEmpty()) {
            return new JsonValidationService.ValidationResult(
                    false, "No chunks found for composition " + compositionId, 0, 0, null);
        }

        List<byte[]> chunks    = new ArrayList<>();
        List<String> checksums = new ArrayList<>();

        for (EquityCompositionChunk m : meta) {
            ChunkWithData row = chunkDao.findWithData(compositionId, m.getChunkSeq())
                    .orElse(null);
            if (row == null) {
                return new JsonValidationService.ValidationResult(
                        false, "Chunk " + m.getChunkSeq() + " missing", 0, 0, null);
            }
            byte[] plain = decompress(row);
            chunks.add(plain);
            checksums.add(ChecksumUtil.sha256Hex(plain));
        }
        return jsonValidator.validateChunks(chunks, checksums);
    }

    public EquityComposition getMetadata(long id) {
        return parentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Composition not found: " + id));
    }

    public EquityComposition getCompleted(long id) {
        EquityComposition e = getMetadata(id);
        if (!STATUS_COMPLETED.equals(e.getStatus())) {
            throw new IllegalStateException(
                    "Composition " + id + " not COMPLETED (current=" + e.getStatus() + ")");
        }
        return e;
    }

    public List<EquityComposition> listByPortfolio(String portfolioId) {
        return parentDao.listCompletedByPortfolio(portfolioId);
    }

    public List<EquityComposition> listVersions(String portfolioId) {
        return parentDao.listVersions(portfolioId);
    }

    public EquityComposition getLatestCompleted(String portfolioId) {
        return parentDao.findLatestCompleted(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No latest COMPLETED composition for portfolio " + portfolioId));
    }

    public EquityComposition getByPortfolioAndDate(String portfolioId, LocalDate asOfDate) {
        return parentDao.findLatestCompletedByPortfolioAndDate(portfolioId, asOfDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No latest COMPLETED composition for " + portfolioId + " on " + asOfDate));
    }

    public List<EquityCompositionChunk> listChunks(long id) {
        return chunkDao.listMetadata(id);
    }

    public Meta readMeta(long id) {
        EquityComposition meta = getCompleted(id);
        Integer metaLength = meta.getMetaLength();
        if (metaLength == null || metaLength <= 0) {
            throw new IllegalStateException("meta_length not recorded for composition " + id);
        }

        ChunkWithData c0 = chunkDao.findWithData(id, 0)
                .orElseThrow(() -> new IllegalStateException("Chunk 0 missing for composition " + id));
        byte[] plain = decompress(c0);

        if (plain.length < PREFIX_LEN + metaLength) {
            throw new IllegalStateException(
                    "Chunk 0 too small to contain meta (len=" + plain.length + ")");
        }
        byte[] metaJson = Arrays.copyOfRange(plain, PREFIX_LEN, PREFIX_LEN + metaLength);
        try {
            return jsonMapper.readValue(metaJson, Meta.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse stored meta block", e);
        }
    }

    public InputStream openUnifiedStream(long id, boolean validateBeforeStreaming) {
        EquityComposition meta = getCompleted(id);

        List<byte[]> chunks    = new ArrayList<>(meta.getTotalChunks());
        List<String> checksums = new ArrayList<>(meta.getTotalChunks());

        for (int seq = 0; seq < meta.getTotalChunks(); seq++) {
            final int s = seq;
            ChunkWithData row = chunkDao.findWithData(id, s)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing chunk " + s + " for composition " + id));
            byte[] plain = decompress(row);

            String actual = ChecksumUtil.sha256Hex(plain);
            if (row.checksum() != null && !row.checksum().equalsIgnoreCase(actual)) {
                throw new IllegalStateException(
                        "Checksum mismatch for chunk " + s + " of composition " + id);
            }
            chunks.add(plain);
            checksums.add(actual);
        }

        if (validateBeforeStreaming) {
            JsonValidationService.ValidationResult vr =
                    jsonValidator.validateChunks(chunks, checksums);
            if (!vr.ok()) {
                throw new IllegalStateException("Stored JSON failed validation: " + vr.message());
            }
            if (meta.getDocumentChecksum() != null &&
                    !meta.getDocumentChecksum().equalsIgnoreCase(vr.checksum())) {
                throw new IllegalStateException(
                        "Document checksum mismatch: recorded=" + meta.getDocumentChecksum() +
                        " recomputed=" + vr.checksum());
            }
        }
        return new ChunkedJsonInputStream(chunks);
    }

    public EquityComposition persistRaw(String rawJson,
                                        String portfolioIdHint,
                                        LocalDate asOfDateHint) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("Request body must be a non-empty JSON document");
        }

        byte[] document = rawJson.getBytes(StandardCharsets.UTF_8);
        int metaLength = findMetaLength(document);

        String portfolioId = portfolioIdHint;
        LocalDate asOfDate = asOfDateHint;
        if (portfolioId == null || asOfDate == null) {
            JsonNode root = jsonMapper.readTree(rawJson);
            JsonNode metaNode = root.path("meta");
            if (portfolioId == null) {
                JsonNode p = metaNode.get("portfolioId");
                if (p != null && p.isString()) portfolioId = p.asString();
                if (portfolioId == null) portfolioId = "UNKNOWN";
            }
            if (asOfDate == null) {
                JsonNode d = metaNode.get("asOfDate");
                if (d != null && d.isString()) asOfDate = LocalDate.parse(d.asString());
                if (asOfDate == null) asOfDate = LocalDate.now();
            }
        }

        int headerLen = PREFIX_LEN + metaLength + MIDDLE_LEN;
        int contentLen = document.length - headerLen - 1;
        int chunk0Length = headerLen + contentLen / 2;

        return persist(document, portfolioId, asOfDate, metaLength, chunk0Length);
    }

    private int findMetaLength(byte[] document) {
        try (JsonParser p = jsonFactory.createParser(
                READ_CONTEXT, new ByteArrayInputStream(document))) {

            if (p.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalArgumentException("root is not a JSON object");
            }
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.PROPERTY_NAME
                        && "meta".equals(p.currentName())) {
                    p.nextToken();
                    int metaStart = PREFIX_LEN;
                    int depth = 1;
                    while (depth > 0 && p.nextToken() != null) {
                        if (p.currentToken() == JsonToken.START_OBJECT ||
                            p.currentToken() == JsonToken.START_ARRAY) depth++;
                        else if (p.currentToken() == JsonToken.END_OBJECT ||
                                 p.currentToken() == JsonToken.END_ARRAY) depth--;
                    }
                    long metaEnd = p.currentLocation().getByteOffset();
                    return (int) (metaEnd - metaStart);
                }
            }
            throw new IllegalArgumentException("no 'meta' object found");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to locate meta block: " + e.getMessage(), e);
        }
    }

    private byte[] decompress(ChunkWithData row) {
        if (!row.compressed()) return row.data();
        try {
            byte[] plain = CompressionUtil.gunzip(row.data());
            if (row.uncompressedSize() > 0 && plain.length != row.uncompressedSize()) {
                throw new IllegalStateException(
                        "Chunk " + row.seq() + " of composition " + row.compositionId() +
                        ": decompressed size mismatch expected=" + row.uncompressedSize() +
                        " actual=" + plain.length);
            }
            return plain;
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to decompress chunk " + row.seq() +
                    " of composition " + row.compositionId(), e);
        }
    }
}
