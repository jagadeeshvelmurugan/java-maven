package com.example.equity.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level payload: metadata plus a list of holdings.
 * Hand-written getters/setters/builders — no Lombok.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquityCompositionPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String  portfolioId;
    private String  portfolioName;
    private LocalDate asOfDate;
    private String  currency;
    private Instant generatedAt;
    private Integer recordCount;
    private String  source;
    private List<EquityHolding> holdings = new ArrayList<>();

    public EquityCompositionPayload() {}

    public EquityCompositionPayload(String portfolioId,
                                    String portfolioName,
                                    LocalDate asOfDate,
                                    String currency,
                                    Instant generatedAt,
                                    Integer recordCount,
                                    String source,
                                    List<EquityHolding> holdings) {
        this.portfolioId   = portfolioId;
        this.portfolioName = portfolioName;
        this.asOfDate      = asOfDate;
        this.currency      = currency;
        this.generatedAt   = generatedAt;
        this.recordCount   = recordCount;
        this.source        = source;
        this.holdings      = holdings == null ? new ArrayList<>() : holdings;
    }

    public String  getPortfolioId()   { return portfolioId; }
    public void    setPortfolioId(String v)   { this.portfolioId = v; }

    public String  getPortfolioName() { return portfolioName; }
    public void    setPortfolioName(String v) { this.portfolioName = v; }

    public LocalDate getAsOfDate()    { return asOfDate; }
    public void    setAsOfDate(LocalDate v)   { this.asOfDate = v; }

    public String  getCurrency()      { return currency; }
    public void    setCurrency(String v)      { this.currency = v; }

    public Instant getGeneratedAt()   { return generatedAt; }
    public void    setGeneratedAt(Instant v)  { this.generatedAt = v; }

    public Integer getRecordCount()   { return recordCount; }
    public void    setRecordCount(Integer v)  { this.recordCount = v; }

    public String  getSource()        { return source; }
    public void    setSource(String v)        { this.source = v; }

    public List<EquityHolding> getHoldings()  { return holdings; }
    public void    setHoldings(List<EquityHolding> v) {
        this.holdings = v == null ? new ArrayList<>() : v;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String  portfolioId;
        private String  portfolioName;
        private LocalDate asOfDate;
        private String  currency;
        private Instant generatedAt;
        private Integer recordCount;
        private String  source;
        private List<EquityHolding> holdings = new ArrayList<>();

        public Builder portfolioId(String v)          { this.portfolioId = v;   return this; }
        public Builder portfolioName(String v)        { this.portfolioName = v; return this; }
        public Builder asOfDate(LocalDate v)          { this.asOfDate = v;      return this; }
        public Builder currency(String v)             { this.currency = v;      return this; }
        public Builder generatedAt(Instant v)         { this.generatedAt = v;   return this; }
        public Builder recordCount(Integer v)         { this.recordCount = v;   return this; }
        public Builder source(String v)               { this.source = v;        return this; }
        public Builder holdings(List<EquityHolding> v){ this.holdings = v == null ? new ArrayList<>() : v; return this; }

        public EquityCompositionPayload build() {
            return new EquityCompositionPayload(portfolioId, portfolioName, asOfDate,
                    currency, generatedAt, recordCount, source, holdings);
        }
    }
}