package com.example.stockmarket.analytics.model;

/**
 * Statistics computed over the requested history window.
 *
 * @param returnPercent           change from the first to the last close, in percent
 * @param dailyVolatilityPercent  standard deviation of period-to-period close returns, in percent
 * @param movingAverage           simple moving average of the last {@code movingAverageWindow} closes
 * @param trend                   "UPTREND" when the last close is above the moving average, otherwise "DOWNTREND"
 */
public record PriceStats(
        double startPrice,
        double endPrice,
        double returnPercent,
        double high,
        double low,
        double averageClose,
        double dailyVolatilityPercent,
        long averageVolume,
        int movingAverageWindow,
        double movingAverage,
        String trend) {
}
