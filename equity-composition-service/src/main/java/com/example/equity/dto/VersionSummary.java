package com.example.equity.dto;

import com.example.equity.domain.EquityComposition;

import java.time.Instant;
import java.time.LocalDate;

public class VersionSummary {

    private final Long id;
    private final Instant versionTimestamp;
    private final String latest;
    private final String status;
    private final LocalDate asOfDate;
    private final Long totalBytes;
    private final Long storedBytes;
    private final String documentChecksum;
    private final String errorMessage;

    private VersionSummary(Builder b) {
        this.id = b.id;
        this.versionTimestamp = b.versionTimestamp;
        this.latest = b.latest;
        this.status = b.status;
        this.asOfDate = b.asOfDate;
        this.totalBytes = b.totalBytes;
        this.storedBytes = b.storedBytes;
        this.documentChecksum = b.documentChecksum;
        this.errorMessage = b.errorMessage;
    }

    public Long getId()                  { return id; }
    public Instant getVersionTimestamp() { return versionTimestamp; }
    public String getLatest()            { return latest; }
    public String getStatus()            { return status; }
    public LocalDate getAsOfDate()       { return asOfDate; }
    public Long getTotalBytes()          { return totalBytes; }
    public Long getStoredBytes()         { return storedBytes; }
    public String getDocumentChecksum()  { return documentChecksum; }
    public String getErrorMessage()      { return errorMessage; }

    public static VersionSummary from(EquityComposition e) {
        return VersionSummary.builder()
                .id(e.getId())
                .versionTimestamp(e.getVersionTimestamp())
                .latest(e.getLatest())
                .status(e.getStatus())
                .asOfDate(e.getAsOfDate())
                .totalBytes(e.getTotalBytes())
                .storedBytes(e.getStoredBytes())
                .documentChecksum(e.getDocumentChecksum())
                .errorMessage(e.getErrorMessage())
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Instant versionTimestamp;
        private String latest;
        private String status;
        private LocalDate asOfDate;
        private Long totalBytes;
        private Long storedBytes;
        private String documentChecksum;
        private String errorMessage;

        public Builder id(Long v)                  { this.id = v; return this; }
        public Builder versionTimestamp(Instant v) { this.versionTimestamp = v; return this; }
        public Builder latest(String v)            { this.latest = v; return this; }
        public Builder status(String v)            { this.status = v; return this; }
        public Builder asOfDate(LocalDate v)       { this.asOfDate = v; return this; }
        public Builder totalBytes(Long v)          { this.totalBytes = v; return this; }
        public Builder storedBytes(Long v)         { this.storedBytes = v; return this; }
        public Builder documentChecksum(String v)  { this.documentChecksum = v; return this; }
        public Builder errorMessage(String v)      { this.errorMessage = v; return this; }

        public VersionSummary build() { return new VersionSummary(this); }
    }
}
