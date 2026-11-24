package com.example.stockmarket.analytics.export;

import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.StockReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF report: page 1 has the summary, metric table and price chart; then one page per stock with
 * its own chart and recent prices.
 */
public final class PdfReportWriter {

    private static final PDRectangle PAGE = PDRectangle.A4;
    private static final float MARGIN = 40;
    private static final float CONTENT_W = PAGE.getWidth() - 2 * MARGIN;
    private static final Color DARK = new Color(0x13, 0x17, 0x22);
    private static final Color MUTED = new Color(0x5F, 0x63, 0x6E);
    private static final Color ACCENT = new Color(0x29, 0x62, 0xFF);
    private static final Color ROW_ALT = new Color(0xF4, 0xF5, 0xF7);
    private static final Color GAIN = new Color(0x1B, 0x7F, 0x5A);
    private static final Color LOSS = new Color(0xC6, 0x28, 0x28);
    private static final int RECENT_ROWS = 15;

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private PdfReportWriter() {
    }

    public static byte[] write(ComparisonReport report) {
        try {
            return new PdfReportWriter().build(report);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build PDF report", e);
        }
    }

    private byte[] build(ComparisonReport report) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(ReportMetrics.title(report));
            info.setCreator("Real-Time Stock Market - Analytics & Reports");

            summaryPage(doc, report);
            if (report.stocks().size() > 1) {
                for (StockReport s : report.stocks()) {
                    stockPage(doc, report, s);
                }
            } else {
                recentPricesPage(doc, report, report.stocks().get(0));
            }

            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) footer(doc, doc.getPage(i), report, i + 1, total);

            doc.save(out);
            return out.toByteArray();
        }
    }

    private void summaryPage(PDDocument doc, ComparisonReport report) throws IOException {
        PDPage page = new PDPage(PAGE);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = banner(cs, ReportMetrics.title(report), ReportMetrics.generatedLine(report));

            y = heading(cs, "Highlights", y - 24);
            for (String line : ReportMetrics.highlights(report)) {
                y = text(cs, regular, 10, DARK, "-  " + line, MARGIN, y - 15);
            }

            y = heading(cs, "Key metrics", y - 26);
            List<String> header = new ArrayList<>();
            header.add("Metric");
            report.stocks().forEach(s -> header.add(s.symbol()));
            List<List<String>> rows = new ArrayList<>();
            int maxChars = switch (report.stocks().size()) {
                case 1 -> 60;
                case 2 -> 38;
                case 3 -> 25;
                default -> 18;
            };
            for (ReportMetrics.Metric m : ReportMetrics.METRICS) {
                List<String> row = new ArrayList<>();
                row.add(m.label());
                report.stocks().forEach(s -> row.add(fit(m.text().apply(s), maxChars)));
                rows.add(row);
            }
            y = table(cs, header, rows, y - 8, 120, true);

            float chartH = Math.min(230, y - MARGIN - 40);
            if (chartH > 120) {
                chart(doc, cs, report, MARGIN, y - 16 - chartH, CONTENT_W, chartH);
            }
        }
    }

    private void stockPage(PDDocument doc, ComparisonReport report, StockReport s) throws IOException {
        PDPage page = new PDPage(PAGE);
        doc.addPage(page);
        ComparisonReport single = new ComparisonReport(report.generatedAt(), report.interval(), report.outputSize(),
                report.dataSource(), List.of(s), s.symbol(), s.symbol(), s.symbol());
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = banner(cs, s.symbol() + " - " + fit(s.quote().name(), 50), ReportMetrics.generatedLine(report));
            y = summaryLine(cs, s, y - 26);
            float chartH = 230;
            chart(doc, cs, single, MARGIN, y - 14 - chartH, CONTENT_W, chartH);
            recentPrices(cs, s, y - 14 - chartH - 26);
        }
    }

    private void recentPricesPage(PDDocument doc, ComparisonReport report, StockReport s) throws IOException {
        PDPage page = new PDPage(PAGE);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = banner(cs, s.symbol() + " - " + fit(s.quote().name(), 50), ReportMetrics.generatedLine(report));
            y = summaryLine(cs, s, y - 26);
            recentPrices(cs, s, y - 26);
        }
    }

    private float summaryLine(PDPageContentStream cs, StockReport s, float y) throws IOException {
        String line = "Last " + ReportMetrics.price(s.quote().price()) + " " + s.quote().currency()
                + "   |   Period return " + ReportMetrics.percent(s.stats().returnPercent())
                + "   |   Range " + ReportMetrics.price(s.stats().low()) + " - " + ReportMetrics.price(s.stats().high());
        return text(cs, bold, 11, s.stats().returnPercent() >= 0 ? GAIN : LOSS, line, MARGIN, y);
    }

    private void recentPrices(PDPageContentStream cs, StockReport s, float y) throws IOException {
        y = heading(cs, "Recent prices (" + s.quote().currency() + ")", y);
        List<PricePoint> h = s.history();
        List<List<String>> rows = new ArrayList<>();
        for (int i = h.size() - 1; i >= Math.max(0, h.size() - RECENT_ROWS); i--) {
            PricePoint p = h.get(i);
            rows.add(List.of(p.datetime(), ReportMetrics.price(p.open()), ReportMetrics.price(p.high()),
                    ReportMetrics.price(p.low()), ReportMetrics.price(p.close()), ReportMetrics.volume(p.volume())));
        }
        table(cs, List.of("Date", "Open", "High", "Low", "Close", "Volume"), rows, y - 8, 110, false);
    }

    // ---------------------------------------------------------------- drawing helpers

    private float banner(PDPageContentStream cs, String title, String subtitle) throws IOException {
        float top = PAGE.getHeight();
        cs.setNonStrokingColor(DARK);
        cs.addRect(0, top - 78, PAGE.getWidth(), 78);
        cs.fill();
        text(cs, bold, 18, Color.WHITE, title, MARGIN, top - 38);
        text(cs, regular, 9, new Color(0xB2, 0xB5, 0xBE), subtitle, MARGIN, top - 58);
        return top - 78;
    }

    private float heading(PDPageContentStream cs, String label, float y) throws IOException {
        return text(cs, bold, 12, ACCENT, label, MARGIN, y);
    }

    private float table(PDPageContentStream cs, List<String> header, List<List<String>> rows,
                        float y, float firstColW, boolean zebra) throws IOException {
        int cols = header.size();
        float otherW = (CONTENT_W - firstColW) / Math.max(1, cols - 1);
        float rowH = 15;

        cs.setNonStrokingColor(DARK);
        cs.addRect(MARGIN, y - rowH + 4, CONTENT_W, rowH);
        cs.fill();
        drawRow(cs, header, y, firstColW, otherW, bold, Color.WHITE);

        for (int r = 0; r < rows.size(); r++) {
            y -= rowH;
            if (zebra ? r % 2 == 1 : r % 2 == 0) {
                cs.setNonStrokingColor(ROW_ALT);
                cs.addRect(MARGIN, y - rowH + 4, CONTENT_W, rowH);
                cs.fill();
            }
            drawRow(cs, rows.get(r), y, firstColW, otherW, regular, DARK);
        }
        return y - rowH;
    }

    private void drawRow(PDPageContentStream cs, List<String> cells, float y, float firstColW, float otherW,
                         PDType1Font font, Color color) throws IOException {
        float size = 9;
        text(cs, font, size, color, cells.get(0), MARGIN + 6, y - 7);
        for (int c = 1; c < cells.size(); c++) {
            String v = cells.get(c);
            float colRight = MARGIN + firstColW + otherW * c - 6;
            float w = font.getStringWidth(safe(v)) / 1000 * size;
            text(cs, font, size, color, v, colRight - w, y - 7);
        }
    }

    private void chart(PDDocument doc, PDPageContentStream cs, ComparisonReport report,
                       float x, float y, float w, float h) throws IOException {
        // Render at 2x for a sharp image when printed.
        PDImageXObject image = LosslessFactory.createFromImage(doc,
                ChartRenderer.render(report, Math.round(w * 2), Math.round(h * 2)));
        cs.drawImage(image, x, y, w, h);
    }

    private void footer(PDDocument doc, PDPage page, ComparisonReport report, int number, int total) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.setStrokingColor(new Color(0xD0, 0xD3, 0xDA));
            cs.moveTo(MARGIN, 34);
            cs.lineTo(PAGE.getWidth() - MARGIN, 34);
            cs.stroke();
            text(cs, regular, 7.5f, report.isDemo() ? LOSS : MUTED, ReportMetrics.disclaimer(report), MARGIN, 22);
            String pageLabel = "Page " + number + " of " + total;
            float w = regular.getStringWidth(pageLabel) / 1000 * 7.5f;
            text(cs, regular, 7.5f, MUTED, pageLabel, PAGE.getWidth() - MARGIN - w, 22);
        }
    }

    /** Draws one line of text and returns its baseline y. */
    private float text(PDPageContentStream cs, PDType1Font font, float size, Color color,
                       String value, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(value));
        cs.endText();
        return y;
    }

    /** Standard PDF fonts only support Latin-1; replace anything else so a company name can never break the export. */
    static String safe(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c >= 32 && c <= 126 || c >= 160 && c <= 255 ? c : '?');
        }
        return sb.toString();
    }

    private static String fit(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
