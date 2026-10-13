package com.example.equity.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A single stock in an equity composition.
 * Hand-written getters, setters, constructors, and builder — no Lombok.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquityHolding implements Serializable {

    private static final long serialVersionUID = 1L;

    private String     isin;
    private String     ticker;
    private String     securityName;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal marketValue;
    private BigDecimal weightPercentage;
    private String     sector;
    private String     country;
    private String     currency;
    private Boolean    active;

    public EquityHolding() {}

    public EquityHolding(String isin,
                         String ticker,
                         String securityName,
                         BigDecimal quantity,
                         BigDecimal price,
                         BigDecimal marketValue,
                         BigDecimal weightPercentage,
                         String sector,
                         String country,
                         String currency,
                         Boolean active) {
        this.isin             = isin;
        this.ticker           = ticker;
        this.securityName     = securityName;
        this.quantity         = quantity;
        this.price            = price;
        this.marketValue      = marketValue;
        this.weightPercentage = weightPercentage;
        this.sector           = sector;
        this.country          = country;
        this.currency         = currency;
        this.active           = active;
    }

    /* ---------- getters / setters ---------- */

    public String     getIsin()             { return isin; }
    public void       setIsin(String v)     { this.isin = v; }

    public String     getTicker()           { return ticker; }
    public void       setTicker(String v)   { this.ticker = v; }

    public String     getSecurityName()     { return securityName; }
    public void       setSecurityName(String v) { this.securityName = v; }

    public BigDecimal getQuantity()         { return quantity; }
    public void       setQuantity(BigDecimal v) { this.quantity = v; }

    public BigDecimal getPrice()            { return price; }
    public void       setPrice(BigDecimal v)    { this.price = v; }

    public BigDecimal getMarketValue()      { return marketValue; }
    public void       setMarketValue(BigDecimal v) { this.marketValue = v; }

    public BigDecimal getWeightPercentage() { return weightPercentage; }
    public void       setWeightPercentage(BigDecimal v) { this.weightPercentage = v; }

    public String     getSector()           { return sector; }
    public void       setSector(String v)   { this.sector = v; }

    public String     getCountry()          { return country; }
    public void       setCountry(String v)  { this.country = v; }

    public String     getCurrency()         { return currency; }
    public void       setCurrency(String v) { this.currency = v; }

    public Boolean    getActive()           { return active; }
    public void       setActive(Boolean v)  { this.active = v; }

    /* ---------- builder ---------- */

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String     isin;
        private String     ticker;
        private String     securityName;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal marketValue;
        private BigDecimal weightPercentage;
        private String     sector;
        private String     country;
        private String     currency;
        private Boolean    active;

        public Builder isin(String v)                 { this.isin = v;             return this; }
        public Builder ticker(String v)               { this.ticker = v;           return this; }
        public Builder securityName(String v)         { this.securityName = v;     return this; }
        public Builder quantity(BigDecimal v)         { this.quantity = v;         return this; }
        public Builder price(BigDecimal v)            { this.price = v;            return this; }
        public Builder marketValue(BigDecimal v)      { this.marketValue = v;      return this; }
        public Builder weightPercentage(BigDecimal v) { this.weightPercentage = v; return this; }
        public Builder sector(String v)               { this.sector = v;           return this; }
        public Builder country(String v)              { this.country = v;          return this; }
        public Builder currency(String v)             { this.currency = v;         return this; }
        public Builder active(Boolean v)              { this.active = v;           return this; }

        public EquityHolding build() {
            return new EquityHolding(isin, ticker, securityName, quantity, price,
                    marketValue, weightPercentage, sector, country, currency, active);
        }
    }
}