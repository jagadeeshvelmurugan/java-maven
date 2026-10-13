package com.example.equity.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompositionRecord implements Serializable {

    private String compositionId;
    private String isin;
    private String ticker;
    private List<MarketData> output = new ArrayList<>();

    public CompositionRecord() {}

    public String getCompositionId()              { return compositionId; }
    public void   setCompositionId(String v)      { this.compositionId = v; }
    public String getIsin()                       { return isin; }
    public void   setIsin(String v)               { this.isin = v; }
    public String getTicker()                     { return ticker; }
    public void   setTicker(String v)             { this.ticker = v; }
    public List<MarketData> getOutput()           { return output; }
    public void   setOutput(List<MarketData> v)   { this.output = v == null ? new ArrayList<>() : v; }
}
