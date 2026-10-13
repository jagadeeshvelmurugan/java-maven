package com.example.equity.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashMap;

public class MarketData implements Serializable {

    private String source;
    private LocalDate tradeDate;
    private String market;
    private LinkedHashMap<String, Object> dynamicValues = new LinkedHashMap<>();

    public MarketData() {}

    public String getSource()                                 { return source; }
    public void   setSource(String v)                         { this.source = v; }
    public LocalDate getTradeDate()                           { return tradeDate; }
    public void   setTradeDate(LocalDate v)                   { this.tradeDate = v; }
    public String getMarket()                                 { return market; }
    public void   setMarket(String v)                         { this.market = v; }
    public LinkedHashMap<String, Object> getDynamicValues()   { return dynamicValues; }
    public void   setDynamicValues(LinkedHashMap<String, Object> v) {
        this.dynamicValues = v == null ? new LinkedHashMap<>() : v;
    }
}
