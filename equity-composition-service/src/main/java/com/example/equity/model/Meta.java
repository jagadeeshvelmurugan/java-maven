package com.example.equity.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class Meta implements Serializable {

    private String  contextId;
    private String  portfolioId;
    private LocalDate asOfDate;
    private Instant generatedAt;
    private String  schemaVersion = "1.0";
    private String  dataSource;
    private String  sourceSystem;

    private int     recordCount;
    private int     rowCount;
    private long    contentBytes;
    private String  contentChecksum;

    private int     chunkSize;
    private int     expectedChunks;

    private Map<String, Object> extras = new LinkedHashMap<>();

    public Meta() {}

    public String getContextId()                       { return contextId; }
    public void   setContextId(String v)               { this.contextId = v; }
    public String getPortfolioId()                     { return portfolioId; }
    public void   setPortfolioId(String v)             { this.portfolioId = v; }
    public LocalDate getAsOfDate()                     { return asOfDate; }
    public void   setAsOfDate(LocalDate v)             { this.asOfDate = v; }
    public Instant getGeneratedAt()                    { return generatedAt; }
    public void   setGeneratedAt(Instant v)            { this.generatedAt = v; }
    public String getSchemaVersion()                   { return schemaVersion; }
    public void   setSchemaVersion(String v)           { this.schemaVersion = v; }
    public String getDataSource()                      { return dataSource; }
    public void   setDataSource(String v)              { this.dataSource = v; }
    public String getSourceSystem()                    { return sourceSystem; }
    public void   setSourceSystem(String v)            { this.sourceSystem = v; }
    public int    getRecordCount()                     { return recordCount; }
    public void   setRecordCount(int v)                { this.recordCount = v; }
    public int    getRowCount()                        { return rowCount; }
    public void   setRowCount(int v)                   { this.rowCount = v; }
    public long   getContentBytes()                    { return contentBytes; }
    public void   setContentBytes(long v)              { this.contentBytes = v; }
    public String getContentChecksum()                 { return contentChecksum; }
    public void   setContentChecksum(String v)         { this.contentChecksum = v; }
    public int    getChunkSize()                       { return chunkSize; }
    public void   setChunkSize(int v)                  { this.chunkSize = v; }
    public int    getExpectedChunks()                  { return expectedChunks; }
    public void   setExpectedChunks(int v)             { this.expectedChunks = v; }
    public Map<String, Object> getExtras()             { return extras; }
    public void   setExtras(Map<String, Object> v)     { this.extras = v == null ? new LinkedHashMap<>() : v; }
}
