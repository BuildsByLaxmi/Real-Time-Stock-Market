package com.example.stockmarket.analytics.export;

import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.StockReport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plain-text report: readable in any editor, e-mail body or chat message. */
public final class TextReportWriter {

    private static final int RECENT_ROWS = 10;

    private TextReportWriter() {
    }

    public static byte[] write(ComparisonReport report) {
        return render(report).getBytes(StandardCharsets.UTF_8);
    }

    public static String render(ComparisonReport report) {
        StringBuilder sb = new StringBuilder();
        String title = ReportMetrics.title(report);
        sb.append(title).append('\n').append("=".repeat(title.length())).append('\n');
        sb.append(ReportMetrics.generatedLine(report)).append("\n\n");

        sb.append("HIGHLIGHTS\n");
        for (String line : ReportMetrics.highlights(report)) sb.append("  * ").append(line).append('\n');
        sb.append('\n');

        sb.append("KEY METRICS\n");
        List<String> header = new ArrayList<>();
        header.add("Metric");
        report.stocks().forEach(s -> header.add(s.symbol()));
        List<List<String>> rows = new ArrayList<>();
        for (ReportMetrics.Metric m : ReportMetrics.METRICS) {
            List<String> row = new ArrayList<>();
            row.add(m.label());
            report.stocks().forEach(s -> row.add(m.text().apply(s)));
            rows.add(row);
        }
        appendTable(sb, header, rows, 1);
        sb.append('\n');

        for (StockReport s : report.stocks()) {
            sb.append("RECENT PRICES - ").append(s.symbol()).append(" (").append(s.quote().currency()).append(")\n");
            List<PricePoint> h = s.history();
            List<List<String>> priceRows = new ArrayList<>();
            for (int i = h.size() - 1; i >= Math.max(0, h.size() - RECENT_ROWS); i--) {
                PricePoint p = h.get(i);
                priceRows.add(List.of(p.datetime(), ReportMetrics.price(p.open()), ReportMetrics.price(p.high()),
                        ReportMetrics.price(p.low()), ReportMetrics.price(p.close()), ReportMetrics.volume(p.volume())));
            }
            appendTable(sb, List.of("Date", "Open", "High", "Low", "Close", "Volume"), priceRows, 1);
            sb.append('\n');
        }

        sb.append(ReportMetrics.disclaimer(report)).append('\n');
        return sb.toString();
    }

    /** Fixed-width table; columns from {@code firstNumericColumn} on are right-aligned. */
    private static void appendTable(StringBuilder sb, List<String> header, List<List<String>> rows, int firstNumericColumn) {
        int cols = header.size();
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) widths[c] = header.get(c).length();
        for (List<String> row : rows) {
            for (int c = 0; c < cols; c++) widths[c] = Math.max(widths[c], row.get(c).length());
        }
        appendRow(sb, header, widths, firstNumericColumn);
        List<String> rule = new ArrayList<>();
        for (int w : widths) rule.add("-".repeat(w));
        appendRow(sb, rule, widths, firstNumericColumn);
        for (List<String> row : rows) appendRow(sb, row, widths, firstNumericColumn);
    }

    private static void appendRow(StringBuilder sb, List<String> cells, int[] widths, int firstNumericColumn) {
        sb.append("  ");
        for (int c = 0; c < cells.size(); c++) {
            String fmt = c >= firstNumericColumn ? "%" + widths[c] + "s" : "%-" + widths[c] + "s";
            sb.append(String.format(Locale.US, fmt, cells.get(c)));
            if (c < cells.size() - 1) sb.append("  ");
        }
        sb.append('\n');
    }
}
