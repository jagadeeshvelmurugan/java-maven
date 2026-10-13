package com.example.equity.entity;

import java.time.Instant;

/**
 * Row of EQUITY_COMPOSITION_CHUNK without the BLOB payload.
 * Hand-written getters, hand-written Builder, no Lombok.
 */
public class EquityCompositionChunkEntity {

    private final Long    compositionId;
    private final Integer chunkSeq;
    private final Integer chunkSize;
    private final Integer uncompressedSize;
    private final String  status;
    private final String  checksum;
    private final Boolean compressed;
    private final String  compression;
    private final Instant createdAt;

    private EquityCompositionChunkEntity(Builder b) {
        this.compositionId     = b.compositionId;
        this.chunkSeq          = b.chunkSeq;
        this.chunkSize         = b.chunkSize;
        this.uncompressedSize  = b.uncompressedSize;
        this.status            = b.status;
        this.checksum          = b.checksum;
        this.compressed        = b.compressed;
        this.compression       = b.compression;
        this.createdAt         = b.createdAt;
    }

    public Long    getCompositionId()      { return compositionId; }
    public Integer getChunkSeq()           { return chunkSeq; }
    public Integer getChunkSize()          { return chunkSize; }
    public Integer getUncompressedSize()   { return uncompressedSize; }
    public String  getStatus()             { return status; }
    public String  getChecksum()           { return checksum; }
    public Boolean getCompressed()         { return compressed; }
    public String  getCompression()        { return compression; }
    public Instant getCreatedAt()          { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long    compositionId;
        private Integer chunkSeq;
        private Integer chunkSize;
        private Integer uncompressedSize;
        private String  status;
        private String  checksum;
        private Boolean compressed;
        private String  compression;
        private Instant createdAt;

        public Builder compositionId(Long v)       { this.compositionId = v;     return this; }
        public Builder chunkSeq(Integer v)         { this.chunkSeq = v;          return this; }
        public Builder chunkSize(Integer v)        { this.chunkSize = v;         return this; }
        public Builder uncompressedSize(Integer v) { this.uncompressedSize = v;  return this; }
        public Builder status(String v)            { this.status = v;            return this; }
        public Builder checksum(String v)          { this.checksum = v;          return this; }
        public Builder compressed(Boolean v)       { this.compressed = v;        return this; }
        public Builder compression(String v)       { this.compression = v;       return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;         return this; }

        public EquityCompositionChunkEntity build() {
            return new EquityCompositionChunkEntity(this);
        }
    }
}