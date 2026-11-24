package com.example.stockmarket.analytics.model;

import java.util.List;

/**
 * Everything the app needs to draw a price chart and the stats panel for one stock.
 *
 * @param history       oldest first
 * @param movingAverage aligned with {@code history}; null until the window is full
 */
public record StockReport(StockQuote quote, PriceStats stats, List<PricePoint> history, List<Double> movingAverage) {

    public String symbol() {
        return quote.symbol();
    }
}
