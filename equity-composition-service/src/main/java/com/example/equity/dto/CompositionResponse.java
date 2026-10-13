package com.example.equity.dto;

import com.example.equity.domain.EquityComposition;

import java.time.Instant;
import java.time.LocalDate;

public class CompositionResponse {

    private final Long id;
    private final String portfolioId;
    private final LocalDate asOfDate;
    private final String status;
    private final Integer totalChunks;
    private final Long totalBytes;
    private final Long storedBytes;
    private final Integer metaLength;
    private final Double compressionRatio;
    private final Boolean compressed;
    private final String documentChecksum;
    private final Instant validatedAt;
    private final String validationMsg;
    private final Instant versionTimestamp;
    private final String latest;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String streamUrl;
    private final String metaUrl;
    private final String validateUrl;
    private final String versionsUrl;

    private CompositionResponse(Builder b) {
        this.id = b.id;
        this.portfolioId = b.portfolioId;
        this.asOfDate = b.asOfDate;
        this.status = b.status;
        this.totalChunks = b.totalChunks;
        this.totalBytes = b.totalBytes;
        this.storedBytes = b.storedBytes;
        this.metaLength = b.metaLength;
        this.compressionRatio = b.compressionRatio;
        this.compressed = b.compressed;
        this.documentChecksum = b.documentChecksum;
        this.validatedAt = b.validatedAt;
        this.validationMsg = b.validationMsg;
        this.versionTimestamp = b.versionTimestamp;
        this.latest = b.latest;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.streamUrl = b.streamUrl;
        this.metaUrl = b.metaUrl;
        this.validateUrl = b.validateUrl;
        this.versionsUrl = b.versionsUrl;
    }

    public Long getId()                 { return id; }
    public String getPortfolioId()      { return portfolioId; }
    public LocalDate getAsOfDate()      { return asOfDate; }
    public String getStatus()           { return status; }
    public Integer getTotalChunks()     { return totalChunks; }
    public Long getTotalBytes()         { return totalBytes; }
    public Long getStoredBytes()        { return storedBytes; }
    public Integer getMetaLength()      { return metaLength; }
    public Double getCompressionRatio() { return compressionRatio; }
    public Boolean getCompressed()      { return compressed; }
    public String getDocumentChecksum() { return documentChecksum; }
    public Instant getValidatedAt()     { return validatedAt; }
    public String getValidationMsg()    { return validationMsg; }
    public Instant getVersionTimestamp(){ return versionTimestamp; }
    public String getLatest()           { return latest; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }
    public String getStreamUrl()        { return streamUrl; }
    public String getMetaUrl()          { return metaUrl; }
    public String getValidateUrl()      { return validateUrl; }
    public String getVersionsUrl()      { return versionsUrl; }

    public static CompositionResponse from(EquityComposition e) {
        double ratio = 1.0;
        if (e.getStoredBytes() != null && e.getStoredBytes() > 0 && e.getTotalBytes() != null) {
            ratio = Math.round(100.0 * e.getTotalBytes() / e.getStoredBytes()) / 100.0;
        }
        boolean compressed = e.getStoredBytes() != null && e.getTotalBytes() != null
                && e.getStoredBytes() < e.getTotalBytes();

        return CompositionResponse.builder()
                .id(e.getId())
                .portfolioId(e.getPortfolioId())
                .asOfDate(e.getAsOfDate())
                .status(e.getStatus())
                .totalChunks(e.getTotalChunks())
                .totalBytes(e.getTotalBytes())
                .storedBytes(e.getStoredBytes())
                .metaLength(e.getMetaLength())
                .compressionRatio(ratio)
                .compressed(compressed)
                .documentChecksum(e.getDocumentChecksum())
                .validatedAt(e.getValidatedAt())
                .validationMsg(e.getValidationMsg())
                .versionTimestamp(e.getVersionTimestamp())
                .latest(e.getLatest())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .streamUrl("/api/v1/equity-compositions/" + e.getId() + "/stream")
                .metaUrl("/api/v1/equity-compositions/" + e.getId() + "/meta")
                .validateUrl("/api/v1/equity-compositions/" + e.getId() + "/validate")
                .versionsUrl(e.getPortfolioId() == null ? null
                        : "/api/v1/equity-compositions/portfolio/" + e.getPortfolioId() + "/versions")
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String portfolioId;
        private LocalDate asOfDate;
        private String status;
        private Integer totalChunks;
        private Long totalBytes;
        private Long storedBytes;
        private Integer metaLength;
        private Double compressionRatio;
        private Boolean compressed;
        private String documentChecksum;
        private Instant validatedAt;
        private String validationMsg;
        private Instant versionTimestamp;
        private String latest;
        private Instant createdAt;
        private Instant updatedAt;
        private String streamUrl;
        private String metaUrl;
        private String validateUrl;
        private String versionsUrl;

        public Builder id(Long v)                  { this.id = v; return this; }
        public Builder portfolioId(String v)       { this.portfolioId = v; return this; }
        public Builder asOfDate(LocalDate v)       { this.asOfDate = v; return this; }
        public Builder status(String v)            { this.status = v; return this; }
        public Builder totalChunks(Integer v)      { this.totalChunks = v; return this; }
        public Builder totalBytes(Long v)          { this.totalBytes = v; return this; }
        public Builder storedBytes(Long v)         { this.storedBytes = v; return this; }
        public Builder metaLength(Integer v)       { this.metaLength = v; return this; }
        public Builder compressionRatio(Double v)  { this.compressionRatio = v; return this; }
        public Builder compressed(Boolean v)       { this.compressed = v; return this; }
        public Builder documentChecksum(String v)  { this.documentChecksum = v; return this; }
        public Builder validatedAt(Instant v)      { this.validatedAt = v; return this; }
        public Builder validationMsg(String v)     { this.validationMsg = v; return this; }
        public Builder versionTimestamp(Instant v) { this.versionTimestamp = v; return this; }
        public Builder latest(String v)            { this.latest = v; return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v)        { this.updatedAt = v; return this; }
        public Builder streamUrl(String v)         { this.streamUrl = v; return this; }
        public Builder metaUrl(String v)           { this.metaUrl = v; return this; }
        public Builder validateUrl(String v)       { this.validateUrl = v; return this; }
        public Builder versionsUrl(String v)       { this.versionsUrl = v; return this; }

        public CompositionResponse build() { return new CompositionResponse(this); }
    }
}
