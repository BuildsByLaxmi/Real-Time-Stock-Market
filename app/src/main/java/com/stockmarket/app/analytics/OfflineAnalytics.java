package com.stockmarket.app.analytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Builds simulated analytics on the phone when the backend is unreachable, so the chart and
 * compare screens stay usable in a demo. Results are marked dataSource = "OFFLINE" and the UI says so.
 * Stats use the same formulas as the backend's AnalyticsService.
 */
public final class OfflineAnalytics {

    private static final Map<String, String[]> PROFILES = new HashMap<>();

    static {
        // symbol -> name, exchange, currency, last price
        put("AAPL", "Apple Inc.", "NASDAQ", "USD", "225.80");
        put("MSFT", "Microsoft Corporation", "NASDAQ", "USD", "428.15");
        put("GOOGL", "Alphabet Inc.", "NASDAQ", "USD", "178.60");
        put("AMZN", "Amazon.com Inc.", "NASDAQ", "USD", "201.40");
        put("META", "Meta Platforms Inc.", "NASDAQ", "USD", "565.40");
        put("TSLA", "Tesla, Inc.", "NASDAQ", "USD", "328.50");
        put("NVDA", "NVIDIA Corporation", "NASDAQ", "USD", "135.20");
        put("NFLX", "Netflix, Inc.", "NASDAQ", "USD", "720.10");
        put("TCS", "Tata Consultancy Services", "NSE", "INR", "4120.50");
        put("INFY", "Infosys Limited", "NSE", "INR", "1880.20");
        put("RELIANCE", "Reliance Industries Ltd", "NSE", "INR", "2950.00");
        put("WIPRO", "Wipro Limited", "NSE", "INR", "540.10");
        put("HDFCBANK", "HDFC Bank Ltd", "NSE", "INR", "1745.00");
        put("TATAMOTORS", "Tata Motors Limited", "NSE", "INR", "980.40");
    }

    private static void put(String symbol, String name, String exchange, String currency, String price) {
        PROFILES.put(symbol, new String[]{name, exchange, currency, price});
    }

    private OfflineAnalytics() {
    }

    public static Comparison compare(List<String> symbols, int bars) {
        Comparison c = new Comparison();
        c.dataSource = "OFFLINE";
        c.stocks = new ArrayList<>();
        for (String s : symbols) c.stocks.add(series(s, bars));
        StockSeries best = c.stocks.get(0), worst = best, calm = best;
        for (StockSeries s : c.stocks) {
            if (s.returnPercent > best.returnPercent) best = s;
            if (s.returnPercent < worst.returnPercent) worst = s;
            if (s.volatilityPercent < calm.volatilityPercent) calm = s;
        }
        c.bestPerformer = best.symbol;
        c.worstPerformer = worst.symbol;
        c.leastVolatile = calm.symbol;
        return c;
    }

    static StockSeries series(String symbol, int bars) {
        String[] p = PROFILES.get(symbol);
        if (p == null) {
            p = new String[]{symbol, "", "USD", Double.toString(50 + Math.floorMod(symbol.hashCode(), 450))};
        }
        Random random = new Random((symbol + "|1day").hashCode());
        double drift = (random.nextDouble() - 0.45) * 0.004;
        double vol = 0.008 + random.nextDouble() * 0.02;

        double[] closes = new double[bars];
        long[] volumes = new long[bars];
        double close = Double.parseDouble(p[3]);
        for (int i = bars - 1; i >= 0; i--) {
            closes[i] = round(close);
            close = close / (1 + drift + random.nextGaussian() * vol);
            volumes[i] = (long) (1_000_000 + random.nextDouble() * 40_000_000);
        }

        StockSeries s = new StockSeries();
        s.symbol = symbol;
        s.name = p[0];
        s.exchange = p[1];
        s.currency = p[2];
        s.closes = closes;
        s.dates = tradingDays(bars);
        s.price = closes[bars - 1];
        s.percentChange = bars > 1 ? round((closes[bars - 1] / closes[bars - 2] - 1) * 100) : 0;
        computeStats(s, volumes);
        return s;
    }

    /** Same definitions as the backend (AnalyticsService.computeStats). */
    static void computeStats(StockSeries s, long[] volumes) {
        double[] c = s.closes;
        int n = c.length;
        int window = Math.max(2, Math.min(20, n / 3));
        s.movingAverage = new double[n];
        double running = 0;
        for (int i = 0; i < n; i++) {
            running += c[i];
            if (i >= window) running -= c[i - window];
            s.movingAverage[i] = i >= window - 1 ? round(running / window) : Double.NaN;
        }

        double hi = c[0], lo = c[0], sum = 0;
        for (double v : c) {
            hi = Math.max(hi, v);
            lo = Math.min(lo, v);
            sum += v;
        }
        double volatility = 0;
        if (n >= 3) {
            double[] r = new double[n - 1];
            double mean = 0;
            for (int i = 1; i < n; i++) {
                r[i - 1] = c[i] / c[i - 1] - 1;
                mean += r[i - 1];
            }
            mean /= r.length;
            double var = 0;
            for (double x : r) var += (x - mean) * (x - mean);
            volatility = Math.sqrt(var / (r.length - 1)) * 100;
        }
        long volSum = 0;
        for (long v : volumes) volSum += v;

        s.startPrice = c[0];
        s.returnPercent = round((c[n - 1] - c[0]) / c[0] * 100);
        s.high = hi;
        s.low = lo;
        s.averageClose = round(sum / n);
        s.volatilityPercent = round(volatility);
        s.averageVolume = volumes.length == 0 ? 0 : volSum / volumes.length;
        s.movingAverageWindow = window;
        s.movingAverageValue = s.movingAverage[n - 1];
        s.trend = c[n - 1] >= s.movingAverageValue ? "UPTREND" : "DOWNTREND";
    }

    private static List<String> tradingDays(int count) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        List<String> out = new ArrayList<>(count);
        while (out.size() < count) {
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) out.add(f.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        Collections.reverse(out);
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
