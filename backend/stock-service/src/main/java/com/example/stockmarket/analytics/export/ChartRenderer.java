package com.example.stockmarket.analytics.export;

import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.StockReport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

/**
 * Draws price charts with plain Java2D (no extra dependency).
 * One stock: close price plus its moving average.
 * Several stocks: performance rebased to 0% at the first bar, so different price levels are comparable.
 */
public final class ChartRenderer {

    /** Series colours; also used by the Android app so a symbol keeps its colour across screens. */
    public static final Color[] PALETTE = {
            new Color(0x29, 0x62, 0xFF), // blue
            new Color(0xF5, 0x7C, 0x00), // orange
            new Color(0x00, 0x96, 0x88), // teal
            new Color(0x8E, 0x24, 0xAA)  // purple
    };
    private static final Color GRID = new Color(0xE3, 0xE6, 0xEC);
    private static final Color AXIS_TEXT = new Color(0x5F, 0x63, 0x6E);
    private static final Color TITLE = new Color(0x13, 0x17, 0x22);

    private ChartRenderer() {
    }

    public static byte[] renderPng(ComparisonReport report, int width, int height) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(render(report, width, height), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedImage render(ComparisonReport report, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            draw(g, report, width, height);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void draw(Graphics2D g, ComparisonReport report, int width, int height) {
        boolean single = report.stocks().size() == 1;
        float scale = Math.max(0.6f, Math.min(width / 900f, height / 450f) * 1.25f);
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(15 * scale));
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(11 * scale));

        // Shared x axis: the union of all bar timestamps (different exchanges can have different holidays).
        TreeSet<String> allDates = new TreeSet<>();
        report.stocks().forEach(s -> s.history().forEach(p -> allDates.add(p.datetime())));
        List<String> xLabels = new ArrayList<>(allDates);
        Map<String, Integer> xIndex = new TreeMap<>();
        for (int i = 0; i < xLabels.size(); i++) xIndex.put(xLabels.get(i), i);

        // Build plotted series.
        List<String> names = new ArrayList<>();
        List<double[][]> series = new ArrayList<>(); // each: [xIndex[], y[]]
        List<Boolean> dashed = new ArrayList<>();
        for (StockReport s : report.stocks()) {
            List<PricePoint> h = s.history();
            double base = h.get(0).close();
            double[] xs = new double[h.size()];
            double[] ys = new double[h.size()];
            for (int i = 0; i < h.size(); i++) {
                xs[i] = xIndex.get(h.get(i).datetime());
                ys[i] = single ? h.get(i).close() : (base == 0 ? 0 : (h.get(i).close() / base - 1) * 100);
            }
            names.add(single ? s.symbol() + " close" : s.symbol());
            series.add(new double[][]{xs, ys});
            dashed.add(false);
        }
        if (single) {
            StockReport s = report.stocks().get(0);
            List<Double> xs = new ArrayList<>();
            List<Double> ys = new ArrayList<>();
            for (int i = 0; i < s.history().size(); i++) {
                Double v = s.movingAverage().get(i);
                if (v != null) {
                    xs.add((double) xIndex.get(s.history().get(i).datetime()));
                    ys.add(v);
                }
            }
            if (!ys.isEmpty()) {
                names.add(s.stats().movingAverageWindow() + "-bar moving avg");
                series.add(new double[][]{xs.stream().mapToDouble(Double::doubleValue).toArray(),
                        ys.stream().mapToDouble(Double::doubleValue).toArray()});
                dashed.add(true);
            }
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[][] s : series) {
            for (double y : s[1]) {
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (min == max) {
            min -= 1;
            max += 1;
        }
        double[] ticks = niceTicks(min, max, 5);
        min = Math.min(min, ticks[0]);
        max = Math.max(max, ticks[ticks.length - 1]);

        // Layout.
        g.setFont(labelFont);
        FontMetrics fm = g.getFontMetrics();
        int pad = Math.round(16 * scale);
        int titleH = Math.round(28 * scale);
        int legendH = Math.round(22 * scale);
        int yLabelW = 0;
        for (double t : ticks) yLabelW = Math.max(yLabelW, fm.stringWidth(formatTick(t, single)));
        int left = pad + yLabelW + Math.round(8 * scale);
        int right = width - pad;
        int top = pad + titleH;
        int bottom = height - pad - legendH - fm.getHeight() - Math.round(6 * scale);
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        // Title.
        g.setColor(TITLE);
        g.setFont(titleFont);
        String title = single
                ? report.stocks().get(0).symbol() + " price (" + report.stocks().get(0).quote().currency() + ")"
                : "Performance since first bar (%)";
        if (report.isDemo()) title += "  [DEMO DATA]";
        g.drawString(title, left, pad + g.getFontMetrics().getAscent());

        // Grid + y labels.
        g.setFont(labelFont);
        for (double t : ticks) {
            int y = (int) Math.round(bottom - (t - min) / (max - min) * plotH);
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(left, y, right, y);
            g.setColor(AXIS_TEXT);
            String label = formatTick(t, single);
            g.drawString(label, left - Math.round(6 * scale) - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }
        if (!single && min < 0 && max > 0) {
            int y0 = (int) Math.round(bottom - (0 - min) / (max - min) * plotH);
            g.setColor(AXIS_TEXT);
            g.drawLine(left, y0, right, y0);
        }

        // X labels: first, middle, last.
        int lastX = Math.max(1, xLabels.size() - 1);
        int[] idx = xLabels.size() > 2 ? new int[]{0, lastX / 2, lastX} : new int[]{0, lastX};
        g.setColor(AXIS_TEXT);
        for (int k = 0; k < idx.length; k++) {
            if (idx[k] >= xLabels.size()) continue;
            String label = shortDate(xLabels.get(idx[k]));
            int x = left + (int) Math.round((double) idx[k] / lastX * plotW);
            int w = fm.stringWidth(label);
            int drawX = k == 0 ? x : (k == idx.length - 1 ? x - w : x - w / 2);
            g.drawString(label, drawX, bottom + fm.getAscent() + Math.round(6 * scale));
        }

        // Lines.
        float lineWidth = Math.max(1.5f, 2.2f * scale);
        for (int s = 0; s < series.size(); s++) {
            double[] xs = series.get(s)[0];
            double[] ys = series.get(s)[1];
            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i < xs.length; i++) {
                double x = left + xs[i] / lastX * plotW;
                double y = bottom - (ys[i] - min) / (max - min) * plotH;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            g.setColor(colorFor(s, single, dashed.get(s)));
            g.setStroke(dashed.get(s)
                    ? new BasicStroke(lineWidth * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[]{6 * scale, 5 * scale}, 0)
                    : new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
        }

        // Legend.
        int lx = left;
        int ly = height - pad - legendH / 2;
        for (int s = 0; s < series.size(); s++) {
            g.setColor(colorFor(s, single, dashed.get(s)));
            g.setStroke(new BasicStroke(lineWidth));
            int sw = Math.round(18 * scale);
            g.drawLine(lx, ly, lx + sw, ly);
            g.setColor(AXIS_TEXT);
            String label = names.get(s);
            g.drawString(label, lx + sw + Math.round(6 * scale), ly + fm.getAscent() / 2 - 1);
            lx += sw + Math.round(22 * scale) + fm.stringWidth(label);
        }
    }

    private static Color colorFor(int index, boolean single, boolean dashed) {
        if (single && dashed) return PALETTE[1];
        return PALETTE[index % PALETTE.length];
    }

    private static String formatTick(double v, boolean price) {
        if (price) return Math.abs(v) >= 1000 ? String.format(Locale.US, "%,.0f", v) : String.format(Locale.US, "%.2f", v);
        return String.format(Locale.US, "%+.0f%%", v);
    }

    private static String shortDate(String datetime) {
        // "2026-11-24" -> "2026-11-24"; "2026-11-24 15:30:00" -> "11-24 15:30"
        return datetime.length() > 10 ? datetime.substring(5, Math.min(16, datetime.length())) : datetime;
    }

    /** About {@code count} evenly spaced round numbers covering [min, max]. */
    static double[] niceTicks(double min, double max, int count) {
        double range = max - min;
        double rough = range / Math.max(1, count - 1);
        double magnitude = Math.pow(10, Math.floor(Math.log10(rough)));
        double residual = rough / magnitude;
        double step = (residual > 5 ? 10 : residual > 2 ? 5 : residual > 1 ? 2 : 1) * magnitude;
        double start = Math.floor(min / step) * step;
        double end = Math.ceil(max / step) * step;
        int n = (int) Math.round((end - start) / step) + 1;
        double[] ticks = new double[n];
        for (int i = 0; i < n; i++) ticks[i] = start + i * step;
        return ticks;
    }
}
