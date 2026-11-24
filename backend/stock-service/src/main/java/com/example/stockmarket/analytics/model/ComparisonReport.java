package com.example.stockmarket.analytics.model;

import java.time.Instant;
import java.util.List;

/**
 * Side-by-side analytics for one or more stocks over the same window. Used by the compare screen
 * and as the single input for every report format (text, PDF, Excel).
 *
 * @param dataSource "TWELVE_DATA" for live data, "DEMO" for simulated data (no API key configured)
 */
public record ComparisonReport(
        Instant generatedAt,
        String interval,
        int outputSize,
        String dataSource,
        List<StockReport> stocks,
        String bestPerformer,
        String worstPerformer,
        String leastVolatile) {

    public boolean isDemo() {
        return "DEMO".equals(dataSource);
    }
}
