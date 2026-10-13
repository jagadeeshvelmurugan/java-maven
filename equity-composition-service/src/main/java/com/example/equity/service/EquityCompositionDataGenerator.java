package com.example.equity.service;

import com.example.equity.model.CompositionRecord;
import com.example.equity.model.Context;
import com.example.equity.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * Builds an in-memory Context with N CompositionRecords, each holding M
 * MarketData rows whose dynamicValues map is a LinkedHashMap.
 *
 * Hand-written Logger — no Lombok.
 */
@Service
public class EquityCompositionDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(EquityCompositionDataGenerator.class);

    private static final String[] SECTORS = {
            "Technology", "Financials", "Healthcare", "Energy", "Industrials",
            "Consumer Discretionary", "Consumer Staples", "Utilities",
            "Real Estate", "Materials", "Communication Services"
    };
    private static final String[] COUNTRIES = {
            "US", "GB", "DE", "FR", "JP", "CN", "IN", "BR", "CA", "AU",
            "CH", "NL", "SE", "SG", "HK", "KR", "IT", "ES", "MX", "ZA"
    };
    private static final String[] CURRENCIES = {
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"
    };
    private static final String[] MARKETS = {
            "NYSE", "NASDAQ", "LSE", "XETRA", "TSE", "HKEX", "ASX", "TSX"
    };

    public Context generateContext(String contextId,
                                   String portfolioId,
                                   LocalDate asOfDate,
                                   int recordCount,
                                   int marketDataPerRecord) {

        if (recordCount <= 0) throw new IllegalArgumentException("recordCount must be > 0");
        if (marketDataPerRecord < 0) throw new IllegalArgumentException("marketDataPerRecord must be >= 0");

        long start = System.currentTimeMillis();

        Context ctx = new Context();
        ctx.setContextId(contextId);
        ctx.setPortfolioId(portfolioId);
        ctx.setAsOfDate(asOfDate);
        ctx.setDataSource("BLOOMBERG");
        ctx.setSourceSystem("EquityCompositionDataGenerator");

        List<CompositionRecord> records = new ArrayList<>(recordCount);
        Random rnd = new Random(42);

        for (int i = 0; i < recordCount; i++) {
            CompositionRecord rec = new CompositionRecord();
            rec.setCompositionId("C" + i);
            rec.setIsin(String.format("ISIN%010d", i));
            rec.setTicker("TCK" + (i % 100_000));

            List<MarketData> output = new ArrayList<>(marketDataPerRecord);
            for (int j = 0; j < marketDataPerRecord; j++) {
                MarketData md = new MarketData();
                md.setSource(rnd.nextInt(4) == 0 ? "REUTERS" : "BLOOMBERG");
                md.setTradeDate(asOfDate);
                md.setMarket(MARKETS[rnd.nextInt(MARKETS.length)]);

                LinkedHashMap<String, Object> dyn = new LinkedHashMap<>();
                long quantity = 100L + rnd.nextInt(5_000_000);
                double price  = 1.0 + rnd.nextDouble() * 999.0;
                double mv     = quantity * price;
                double weight = rnd.nextDouble() * 100.0;

                dyn.put("price",            round2(price));
                dyn.put("bid",              round2(price - 0.05));
                dyn.put("ask",              round2(price + 0.05));
                dyn.put("quantity",         quantity);
                dyn.put("marketValue",      round2(mv));
                dyn.put("weightPercentage", round4(weight));
                dyn.put("sector",           SECTORS[rnd.nextInt(SECTORS.length)]);
                dyn.put("country",          COUNTRIES[rnd.nextInt(COUNTRIES.length)]);
                dyn.put("currency",         CURRENCIES[rnd.nextInt(CURRENCIES.length)]);
                dyn.put("active",           rnd.nextInt(20) != 0);

                md.setDynamicValues(dyn);
                output.add(md);
            }

            rec.setOutput(output);
            records.add(rec);
        }

        ctx.setRecords(records);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Built Context id={} portfolio={} with {} records x {} market data rows in {} ms",
                contextId, portfolioId, recordCount, marketDataPerRecord, elapsed);

        return ctx;
    }

    private static BigDecimal round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}