package com.example.stockmarket.analytics.export;

import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.StockReport;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The comparison table shared by every report format, so text, PDF and Excel always show the same figures.
 * Text output is plain ASCII on purpose: the PDF uses standard fonts, which cannot draw symbols such as the rupee sign.
 */
public final class ReportMetrics {

    /** How Excel should format the numeric value. */
    public enum Kind { TEXT, PRICE, PERCENT, VOLUME }

    /**
     * @param numeric value for Excel; null means "use the text value"
     */
    public record Metric(String label, Kind kind, Function<StockReport, String> text, Function<StockReport, Double> numeric) {
    }

    public static final List<Metric> METRICS = List.of(
            new Metric("Company", Kind.TEXT, r -> r.quote().name(), r -> null),
            new Metric("Exchange", Kind.TEXT, r -> blankToDash(r.quote().exchange()), r -> null),
            new Metric("Currency", Kind.TEXT, r -> r.quote().currency(), r -> null),
            new Metric("Last price", Kind.PRICE, r -> price(r.quote().price()), r -> r.quote().price()),
            new Metric("Day change", Kind.PERCENT, r -> percent(r.quote().percentChange()), r -> r.quote().percentChange() / 100),
            new Metric("Period start price", Kind.PRICE, r -> price(r.stats().startPrice()), r -> r.stats().startPrice()),
            new Metric("Period return", Kind.PERCENT, r -> percent(r.stats().returnPercent()), r -> r.stats().returnPercent() / 100),
            new Metric("Period high", Kind.PRICE, r -> price(r.stats().high()), r -> r.stats().high()),
            new Metric("Period low", Kind.PRICE, r -> price(r.stats().low()), r -> r.stats().low()),
            new Metric("Average close", Kind.PRICE, r -> price(r.stats().averageClose()), r -> r.stats().averageClose()),
            new Metric("Volatility (per bar)", Kind.PERCENT,
                    r -> String.format(Locale.US, "%.2f%%", r.stats().dailyVolatilityPercent()),
                    r -> r.stats().dailyVolatilityPercent() / 100),
            new Metric("Moving average", Kind.PRICE,
                    r -> price(r.stats().movingAverage()) + " (" + r.stats().movingAverageWindow() + ")",
                    r -> r.stats().movingAverage()),
            new Metric("Trend vs average", Kind.TEXT, r -> r.stats().trend(), r -> null),
            new Metric("52-week high", Kind.PRICE, r -> nullablePrice(r.quote().fiftyTwoWeekHigh()), r -> r.quote().fiftyTwoWeekHigh()),
            new Metric("52-week low", Kind.PRICE, r -> nullablePrice(r.quote().fiftyTwoWeekLow()), r -> r.quote().fiftyTwoWeekLow()),
            new Metric("Average volume", Kind.VOLUME, r -> volume(r.stats().averageVolume()), r -> (double) r.stats().averageVolume()));

    private static final DateTimeFormatter GENERATED =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm 'UTC'", Locale.US).withZone(ZoneId.of("UTC"));

    private ReportMetrics() {
    }

    public static String title(ComparisonReport report) {
        return report.stocks().size() == 1
                ? "Stock Report: " + report.stocks().get(0).symbol()
                : "Stock Comparison: " + String.join(" vs ", report.stocks().stream().map(StockReport::symbol).toList());
    }

    public static String generatedLine(ComparisonReport report) {
        return "Generated " + GENERATED.format(report.generatedAt())
                + " | Interval " + report.interval() + " | " + report.outputSize() + " bars"
                + " | Source " + (report.isDemo() ? "DEMO (simulated data)" : "Twelve Data");
    }

    /** Short plain-language conclusions, one per line. */
    public static List<String> highlights(ComparisonReport report) {
        if (report.stocks().size() == 1) {
            StockReport r = report.stocks().get(0);
            return List.of(
                    r.symbol() + " moved " + percent(r.stats().returnPercent()) + " over the period, from "
                            + price(r.stats().startPrice()) + " to " + price(r.stats().endPrice()) + ".",
                    "Trading range: " + price(r.stats().low()) + " - " + price(r.stats().high()) + ".",
                    "Last close is " + ("UPTREND".equals(r.stats().trend()) ? "above" : "below")
                            + " its " + r.stats().movingAverageWindow() + "-bar moving average ("
                            + price(r.stats().movingAverage()) + ").");
        }
        StockReport best = find(report, report.bestPerformer());
        StockReport worst = find(report, report.worstPerformer());
        StockReport calm = find(report, report.leastVolatile());
        return List.of(
                "Best performer: " + best.symbol() + " (" + percent(best.stats().returnPercent()) + ").",
                "Weakest performer: " + worst.symbol() + " (" + percent(worst.stats().returnPercent()) + ").",
                "Least volatile: " + calm.symbol() + " (" + String.format(Locale.US, "%.2f%%", calm.stats().dailyVolatilityPercent())
                        + " per bar).",
                "Returns are price-only and compared in each stock's own currency.");
    }

    public static String disclaimer(ComparisonReport report) {
        return (report.isDemo()
                ? "DEMO DATA: prices are simulated because no market-data API key is configured. "
                : "Market data from Twelve Data; it may be delayed. ")
                + "For academic use only, not investment advice.";
    }

    public static String fileName(ComparisonReport report, String extension) {
        String symbols = String.join("_", report.stocks().stream().map(StockReport::symbol).toList());
        return (report.stocks().size() == 1 ? "stock-report_" : "stock-comparison_") + symbols + "." + extension;
    }

    // ---------------------------------------------------------------- formatting

    public static String price(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    public static String nullablePrice(Double v) {
        return v == null ? "-" : price(v);
    }

    public static String percent(double v) {
        return String.format(Locale.US, "%+.2f%%", v);
    }

    public static String volume(long v) {
        if (v >= 1_000_000_000) return String.format(Locale.US, "%.2fB", v / 1e9);
        if (v >= 1_000_000) return String.format(Locale.US, "%.2fM", v / 1e6);
        if (v >= 1_000) return String.format(Locale.US, "%.1fK", v / 1e3);
        return Long.toString(v);
    }

    private static String blankToDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static StockReport find(ComparisonReport report, String symbol) {
        return report.stocks().stream().filter(s -> s.symbol().equals(symbol)).findFirst()
                .orElse(report.stocks().get(0));
    }
}
