package com.example.equity.controller;

import com.example.equity.domain.EquityComposition;
import com.example.equity.domain.EquityCompositionChunk;
import com.example.equity.dto.CompositionResponse;
import com.example.equity.dto.VersionSummary;
import com.example.equity.model.Context;
import com.example.equity.model.Meta;
import com.example.equity.service.EquityCompositionService;
import com.example.equity.service.JsonValidationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/equity-compositions")
public class EquityCompositionController {

    private final EquityCompositionService service;

    public EquityCompositionController(EquityCompositionService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public ResponseEntity<CompositionResponse> generate(@RequestBody Context ctx) throws IOException {
        EquityComposition saved = service.generateAndPersist(ctx);
        return ResponseEntity.ok(CompositionResponse.from(saved));
    }

    @PostMapping(value = "/persist",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<CompositionResponse> persist(
            @RequestBody String rawJson,
            @RequestParam(value = "portfolioId", required = false) String portfolioId,
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        EquityComposition saved = service.persistRaw(rawJson, portfolioId, asOfDate);
        return ResponseEntity.ok(CompositionResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompositionResponse> metadata(@PathVariable Long id) {
        return ResponseEntity.ok(CompositionResponse.from(service.getMetadata(id)));
    }

    @GetMapping("/{id}/meta")
    public ResponseEntity<Meta> meta(@PathVariable Long id) {
        return ResponseEntity.ok(service.readMeta(id));
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<Map<String, Object>>> chunks(@PathVariable Long id) {
        List<EquityCompositionChunk> chunks = service.listChunks(id);
        return ResponseEntity.ok(chunks.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chunkSeq",         c.getChunkSeq());
            m.put("chunkSize",        c.getChunkSize());
            m.put("uncompressedSize", c.getUncompressedSize());
            m.put("compressed",       c.getCompressed());
            m.put("compression",      c.getCompression());
            m.put("status",           c.getStatus());
            m.put("checksum",         c.getChecksum() == null ? "" : c.getChecksum());
            if (Boolean.TRUE.equals(c.getCompressed())
                    && c.getUncompressedSize() != null
                    && c.getChunkSize() != null && c.getChunkSize() > 0) {
                m.put("ratio", String.format("%.2f",
                        (double) c.getUncompressedSize() / c.getChunkSize()));
            }
            return m;
        }).toList());
    }

    @GetMapping("/portfolio/{portfolioId}/versions")
    public ResponseEntity<List<VersionSummary>> listVersions(@PathVariable String portfolioId) {
        return ResponseEntity.ok(service.listVersions(portfolioId).stream()
                .map(VersionSummary::from).toList());
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<CompositionResponse>> listByPortfolio(@PathVariable String portfolioId) {
        return ResponseEntity.ok(service.listByPortfolio(portfolioId).stream()
                .map(CompositionResponse::from).toList());
    }

    @GetMapping("/portfolio/{portfolioId}/latest")
    public ResponseEntity<CompositionResponse> latestMetadata(@PathVariable String portfolioId) {
        return ResponseEntity.ok(CompositionResponse.from(service.getLatestCompleted(portfolioId)));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateStored(@PathVariable Long id) {
        JsonValidationService.ValidationResult r = service.validateStored(id);
        EquityComposition meta = service.getMetadata(id);
        boolean fingerprintMatch = r.ok() && r.checksum() != null &&
                meta.getDocumentChecksum() != null &&
                r.checksum().equalsIgnoreCase(meta.getDocumentChecksum());
        return ResponseEntity.status(r.ok() ? 200 : 409).body(Map.of(
                "compositionId", id,
                "ok", r.ok(),
                "message", r.message(),
                "bytes", r.bytes(),
                "tokens", r.tokens(),
                "checksum", r.checksum() == null ? "" : r.checksum(),
                "recordedChecksum", meta.getDocumentChecksum() == null ? "" : meta.getDocumentChecksum(),
                "fingerprintMatch", fingerprintMatch,
                "validatedAt", Instant.now().toString()
        ));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable Long id,
            @RequestParam(value = "validate", defaultValue = "true") boolean validate) {
        EquityComposition meta = service.getCompleted(id);

        StreamingResponseBody body = output -> {
            try (InputStream in = service.openUnifiedStream(id, validate)) {
                in.transferTo(output);
                output.flush();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(meta.getTotalBytes());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"equity-composition-" + id + ".json\"");
        headers.set("X-Total-Chunks", String.valueOf(meta.getTotalChunks()));
        headers.set("X-Meta-Length",
                meta.getMetaLength() == null ? "" : String.valueOf(meta.getMetaLength()));
        headers.set("X-Document-Checksum",
                meta.getDocumentChecksum() == null ? "" : meta.getDocumentChecksum());
        headers.set("X-Version-Timestamp",
                meta.getVersionTimestamp() == null ? "" : meta.getVersionTimestamp().toString());
        headers.set("X-Latest", meta.getLatest() == null ? "N" : meta.getLatest());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping(value = "/portfolio/{portfolioId}/latest/stream",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> streamLatest(
            @PathVariable String portfolioId,
            @RequestParam(value = "validate", defaultValue = "true") boolean validate) {
        return stream(service.getLatestCompleted(portfolioId).getId(), validate);
    }

    @GetMapping(value = "/portfolio/{portfolioId}/as-of/{asOfDate}/stream",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> streamByPortfolioAndDate(
            @PathVariable String portfolioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(value = "validate", defaultValue = "true") boolean validate) {
        return stream(service.getByPortfolioAndDate(portfolioId, asOfDate).getId(), validate);
    }
}
