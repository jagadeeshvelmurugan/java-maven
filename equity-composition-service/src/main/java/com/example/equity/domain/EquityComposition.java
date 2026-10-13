package com.example.equity.domain;

import java.time.Instant;
import java.time.LocalDate;

public class EquityComposition {

    private final Long id;
    private final String portfolioId;
    private final LocalDate asOfDate;
    private final String status;
    private final Integer totalChunks;
    private final Long totalBytes;
    private final Long storedBytes;
    private final Integer metaLength;
    private final String documentChecksum;
    private final Instant validatedAt;
    private final String validationMsg;
    private final String errorMessage;
    private final Instant versionTimestamp;
    private final String latest;
    private final Instant createdAt;
    private final Instant updatedAt;

    private EquityComposition(Builder b) {
        this.id = b.id;
        this.portfolioId = b.portfolioId;
        this.asOfDate = b.asOfDate;
        this.status = b.status;
        this.totalChunks = b.totalChunks;
        this.totalBytes = b.totalBytes;
        this.storedBytes = b.storedBytes;
        this.metaLength = b.metaLength;
        this.documentChecksum = b.documentChecksum;
        this.validatedAt = b.validatedAt;
        this.validationMsg = b.validationMsg;
        this.errorMessage = b.errorMessage;
        this.versionTimestamp = b.versionTimestamp;
        this.latest = b.latest;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public Long getId()                 { return id; }
    public String getPortfolioId()      { return portfolioId; }
    public LocalDate getAsOfDate()      { return asOfDate; }
    public String getStatus()           { return status; }
    public Integer getTotalChunks()     { return totalChunks; }
    public Long getTotalBytes()         { return totalBytes; }
    public Long getStoredBytes()        { return storedBytes; }
    public Integer getMetaLength()      { return metaLength; }
    public String getDocumentChecksum() { return documentChecksum; }
    public Instant getValidatedAt()     { return validatedAt; }
    public String getValidationMsg()    { return validationMsg; }
    public String getErrorMessage()     { return errorMessage; }
    public Instant getVersionTimestamp(){ return versionTimestamp; }
    public String getLatest()           { return latest; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }

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
        private String documentChecksum;
        private Instant validatedAt;
        private String validationMsg;
        private String errorMessage;
        private Instant versionTimestamp;
        private String latest;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long v)                     { this.id = v; return this; }
        public Builder portfolioId(String v)          { this.portfolioId = v; return this; }
        public Builder asOfDate(LocalDate v)          { this.asOfDate = v; return this; }
        public Builder status(String v)               { this.status = v; return this; }
        public Builder totalChunks(Integer v)         { this.totalChunks = v; return this; }
        public Builder totalBytes(Long v)             { this.totalBytes = v; return this; }
        public Builder storedBytes(Long v)            { this.storedBytes = v; return this; }
        public Builder metaLength(Integer v)          { this.metaLength = v; return this; }
        public Builder documentChecksum(String v)     { this.documentChecksum = v; return this; }
        public Builder validatedAt(Instant v)         { this.validatedAt = v; return this; }
        public Builder validationMsg(String v)        { this.validationMsg = v; return this; }
        public Builder errorMessage(String v)         { this.errorMessage = v; return this; }
        public Builder versionTimestamp(Instant v)    { this.versionTimestamp = v; return this; }
        public Builder latest(String v)               { this.latest = v; return this; }
        public Builder createdAt(Instant v)           { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v)           { this.updatedAt = v; return this; }

        public EquityComposition build() { return new EquityComposition(this); }
    }
}
