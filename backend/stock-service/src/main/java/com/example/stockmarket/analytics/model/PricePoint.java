package com.example.stockmarket.analytics.model;

/** One candle of price history. {@code datetime} is "yyyy-MM-dd" for daily data, or includes a time for intraday. */
public record PricePoint(String datetime, double open, double high, double low, double close, long volume) {
}
