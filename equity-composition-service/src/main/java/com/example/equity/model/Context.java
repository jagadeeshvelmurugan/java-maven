package com.example.equity.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Context implements Serializable {

    private String contextId;
    private String portfolioId;
    private LocalDate asOfDate;
    private String dataSource;
    private String sourceSystem;
    private Map<String, Object> extras = new LinkedHashMap<>();
    private List<CompositionRecord> records = new ArrayList<>();

    public Context() {}

    public String getContextId()                        { return contextId; }
    public void   setContextId(String v)                { this.contextId = v; }
    public String getPortfolioId()                      { return portfolioId; }
    public void   setPortfolioId(String v)              { this.portfolioId = v; }
    public LocalDate getAsOfDate()                      { return asOfDate; }
    public void   setAsOfDate(LocalDate v)              { this.asOfDate = v; }
    public String getDataSource()                       { return dataSource; }
    public void   setDataSource(String v)               { this.dataSource = v; }
    public String getSourceSystem()                     { return sourceSystem; }
    public void   setSourceSystem(String v)             { this.sourceSystem = v; }
    public Map<String, Object> getExtras()              { return extras; }
    public void   setExtras(Map<String, Object> v)      { this.extras = v == null ? new LinkedHashMap<>() : v; }
    public List<CompositionRecord> getRecords()         { return records; }
    public void   setRecords(List<CompositionRecord> v) { this.records = v == null ? new ArrayList<>() : v; }
}
