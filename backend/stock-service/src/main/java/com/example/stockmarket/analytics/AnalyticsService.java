package com.example.stockmarket.analytics;

import com.example.stockmarket.analytics.data.DemoMarketDataSource;
import com.example.stockmarket.analytics.data.MarketData;
import com.example.stockmarket.analytics.data.MarketDataSource;
import com.example.stockmarket.analytics.data.TwelveDataSource;
import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.PriceStats;
import com.example.stockmarket.analytics.model.StockReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Analytics & Reports (Member 4): price history, statistics and multi-stock comparison.
 * Uses live Twelve Data when TWELVE_DATA_API_KEY is set, otherwise clearly-labelled demo data.
 */
@Service
public class AnalyticsService {

    public static final int MAX_SYMBOLS = 4;
    public static final int MIN_OUTPUT_SIZE = 5;
    public static final int MAX_OUTPUT_SIZE = 500;
    public static final Set<String> INTERVALS =
            Set.of("1min", "5min", "15min", "30min", "1h", "1day", "1week", "1month");

    private static final Pattern SYMBOL = Pattern.compile("^[A-Z0-9][A-Z0-9.\\-]{0,14}$");
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private record CacheEntry(MarketData data, Instant fetchedAt) {
    }

    private final MarketDataSource source;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public AnalyticsService(RestTemplate restTemplate,
                            @Value("${stock.api.key:}") String apiKey,
                            @Value("${analytics.demo-data:false}") boolean forceDemoData) {
        this(forceDemoData || apiKey == null || apiKey.isBlank()
                ? new DemoMarketDataSource()
                : new TwelveDataSource(restTemplate, apiKey), Clock.systemUTC());
    }

    AnalyticsService(MarketDataSource source, Clock clock) {
        this.source = source;
        this.clock = clock;
    }

    public String dataSourceName() {
        return source.name();
    }

    /** Price history + stats for one symbol (drives the price chart screen). */
    public StockReport history(String symbol, String interval, int outputSize) {
        String s = normalizeSymbol(symbol);
        validateWindow(interval, outputSize);
        return buildReport(load(s, interval, outputSize));
    }

    /** Side-by-side analytics for 1..4 comma-separated symbols. */
    public ComparisonReport compare(String symbolsCsv, String interval, int outputSize) {
        List<String> symbols = parseSymbols(symbolsCsv);
        validateWindow(interval, outputSize);

        List<StockReport> stocks = new ArrayList<>();
        for (String s : symbols) {
            stocks.add(buildReport(load(s, interval, outputSize)));
        }

        String best = stocks.stream()
                .max(Comparator.comparingDouble(r -> r.stats().returnPercent())).map(StockReport::symbol).orElse(null);
        String worst = stocks.stream()
                .min(Comparator.comparingDouble(r -> r.stats().returnPercent())).map(StockReport::symbol).orElse(null);
        String calmest = stocks.stream()
                .min(Comparator.comparingDouble(r -> r.stats().dailyVolatilityPercent())).map(StockReport::symbol).orElse(null);

        return new ComparisonReport(Instant.now(clock), interval, outputSize, source.name(),
                List.copyOf(stocks), best, worst, calmest);
    }

    // ---------------------------------------------------------------- validation

    static List<String> parseSymbols(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("Provide at least one symbol, e.g. symbols=AAPL,MSFT");
        }
        List<String> symbols = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AnalyticsService::normalizeSymbol)
                .distinct()
                .toList();
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one symbol, e.g. symbols=AAPL,MSFT");
        }
        if (symbols.size() > MAX_SYMBOLS) {
            throw new IllegalArgumentException("You can compare at most " + MAX_SYMBOLS + " symbols at once");
        }
        return symbols;
    }

    static String normalizeSymbol(String symbol) {
        String s = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL.matcher(s).matches()) {
            throw new IllegalArgumentException("Invalid symbol '" + symbol + "'");
        }
        return s;
    }

    static void validateWindow(String interval, int outputSize) {
        if (!INTERVALS.contains(interval)) {
            throw new IllegalArgumentException("Invalid interval '" + interval + "'. Use one of " + INTERVALS);
        }
        if (outputSize < MIN_OUTPUT_SIZE || outputSize > MAX_OUTPUT_SIZE) {
            throw new IllegalArgumentException(
                    "outputsize must be between " + MIN_OUTPUT_SIZE + " and " + MAX_OUTPUT_SIZE);
        }
    }

    // ---------------------------------------------------------------- data + stats

    private MarketData load(String symbol, String interval, int outputSize) {
        String key = symbol + "|" + interval + "|" + outputSize;
        Instant now = Instant.now(clock);
        CacheEntry hit = cache.get(key);
        if (hit != null && hit.fetchedAt().plus(CACHE_TTL).isAfter(now)) {
            return hit.data();
        }
        MarketData data = source.fetch(symbol, interval, outputSize);
        cache.put(key, new CacheEntry(data, now));
        if (cache.size() > 200) {
            cache.entrySet().removeIf(e -> e.getValue().fetchedAt().plus(CACHE_TTL).isBefore(now));
        }
        return data;
    }

    static StockReport buildReport(MarketData data) {
        List<PricePoint> h = data.history();
        int n = h.size();
        int window = Math.max(2, Math.min(20, n / 3));

        List<Double> sma = new ArrayList<>(Collections.nCopies(n, (Double) null));
        double running = 0;
        for (int i = 0; i < n; i++) {
            running += h.get(i).close();
            if (i >= window) running -= h.get(i - window).close();
            if (i >= window - 1) sma.set(i, round(running / window));
        }

        return new StockReport(data.quote(), computeStats(h, window, sma), List.copyOf(h),
                Collections.unmodifiableList(sma));
    }

    static PriceStats computeStats(List<PricePoint> h, int window, List<Double> sma) {
        int n = h.size();
        double start = h.get(0).close();
        double end = h.get(n - 1).close();
        double high = h.stream().mapToDouble(PricePoint::high).max().orElse(end);
        double low = h.stream().mapToDouble(PricePoint::low).min().orElse(end);
        double avg = h.stream().mapToDouble(PricePoint::close).average().orElse(end);
        long avgVolume = Math.round(h.stream().mapToLong(PricePoint::volume).average().orElse(0));

        double volatility = 0;
        if (n >= 3) {
            double[] returns = new double[n - 1];
            for (int i = 1; i < n; i++) {
                double prev = h.get(i - 1).close();
                returns[i - 1] = prev == 0 ? 0 : h.get(i).close() / prev - 1;
            }
            double mean = Arrays.stream(returns).average().orElse(0);
            double variance = Arrays.stream(returns).map(r -> (r - mean) * (r - mean)).sum() / (returns.length - 1);
            volatility = Math.sqrt(variance) * 100;
        }

        Double lastSma = sma.get(n - 1);
        double ma = lastSma == null ? avg : lastSma;
        return new PriceStats(
                start, end,
                round(start == 0 ? 0 : (end - start) / start * 100),
                high, low, round(avg),
                round(volatility),
                avgVolume,
                window, ma,
                end >= ma ? "UPTREND" : "DOWNTREND");
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
