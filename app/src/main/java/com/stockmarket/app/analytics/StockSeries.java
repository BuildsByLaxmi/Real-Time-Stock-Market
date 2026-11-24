package com.stockmarket.app.analytics;

import java.util.List;

/** Price history + stats for one stock, as returned by GET /api/analytics/history (or built offline). */
public class StockSeries {
    public String symbol;
    public String name;
    public String exchange;
    public String currency;
    public double price;
    public double percentChange;

    /** Oldest first. */
    public List<String> dates;
    public double[] closes;
    /** Same length as {@code closes}; NaN until the moving-average window is full. */
    public double[] movingAverage;

    public double startPrice;
    public double returnPercent;
    public double high;
    public double low;
    public double averageClose;
    public double volatilityPercent;
    public long averageVolume;
    public int movingAverageWindow;
    public double movingAverageValue;
    public String trend;
}
