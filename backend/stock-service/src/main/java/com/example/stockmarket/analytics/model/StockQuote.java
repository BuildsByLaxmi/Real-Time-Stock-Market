package com.example.stockmarket.analytics.model;

/** Latest quote for one symbol. The 52-week values can be null when the data source does not provide them. */
public record StockQuote(
        String symbol,
        String name,
        String exchange,
        String currency,
        double price,
        double change,
        double percentChange,
        double open,
        double high,
        double low,
        double previousClose,
        long volume,
        Double fiftyTwoWeekHigh,
        Double fiftyTwoWeekLow) {
}
