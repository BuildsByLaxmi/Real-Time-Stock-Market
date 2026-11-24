package com.stockmarket.app.analytics;

import java.util.Locale;

/**
 * Plain-text summary built on the phone from what is on screen. Used for "Share as text"
 * and as the fallback when the report server is unreachable.
 */
public final class TextReport {

    private TextReport() {
    }

    public static String build(Comparison c) {
        StringBuilder sb = new StringBuilder();
        if (c.stocks.size() == 1) {
            sb.append("Stock Report: ").append(c.stocks.get(0).symbol);
        } else {
            sb.append("Stock Comparison: ");
            for (int i = 0; i < c.stocks.size(); i++) {
                if (i > 0) sb.append(" vs ");
                sb.append(c.stocks.get(i).symbol);
            }
        }
        sb.append('\n').append(c.sourceLabel()).append("\n\n");

        for (StockSeries s : c.stocks) {
            sb.append(s.symbol).append(" - ").append(s.name).append(" (").append(s.currency).append(")\n");
            line(sb, "Last price", money(s.price));
            line(sb, "Day change", pct(s.percentChange));
            line(sb, "Period return (" + s.closes.length + " days)", pct(s.returnPercent));
            line(sb, "Period range", money(s.low) + " - " + money(s.high));
            line(sb, "Average close", money(s.averageClose));
            line(sb, "Volatility (daily)", String.format(Locale.US, "%.2f%%", s.volatilityPercent));
            line(sb, s.movingAverageWindow + "-day moving avg", money(s.movingAverageValue)
                    + ("UPTREND".equals(s.trend) ? " (price above)" : " (price below)"));
            sb.append('\n');
        }

        if (c.stocks.size() > 1) {
            sb.append("Best performer: ").append(c.bestPerformer).append('\n');
            sb.append("Weakest performer: ").append(c.worstPerformer).append('\n');
            sb.append("Least volatile: ").append(c.leastVolatile).append("\n\n");
        }
        sb.append("For academic use only, not investment advice.\n");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String label, String value) {
        sb.append(String.format(Locale.US, "  %-24s %s%n", label + ":", value));
    }

    public static String money(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    public static String pct(double v) {
        return String.format(Locale.US, "%+.2f%%", v);
    }
}
