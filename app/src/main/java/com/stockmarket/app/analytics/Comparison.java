package com.stockmarket.app.analytics;

import java.util.List;

/** Side-by-side analytics, as returned by GET /api/analytics/compare (or built offline). */
public class Comparison {
    /** "TWELVE_DATA", "DEMO" (server has no API key) or "OFFLINE" (server unreachable, built on the phone). */
    public String dataSource;
    public List<StockSeries> stocks;
    public String bestPerformer;
    public String worstPerformer;
    public String leastVolatile;

    public boolean isLive() {
        return "TWELVE_DATA".equals(dataSource);
    }

    public String sourceLabel() {
        if ("TWELVE_DATA".equals(dataSource)) return "Live data · Twelve Data";
        if ("DEMO".equals(dataSource)) return "Demo data from server (no API key configured)";
        return "Offline demo data · start the backend for live prices";
    }
}
