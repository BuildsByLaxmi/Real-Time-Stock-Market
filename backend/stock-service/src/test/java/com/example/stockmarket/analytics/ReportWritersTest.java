package com.example.stockmarket.analytics;

import com.example.stockmarket.analytics.data.DemoMarketDataSource;
import com.example.stockmarket.analytics.export.ChartRenderer;
import com.example.stockmarket.analytics.export.ExcelReportWriter;
import com.example.stockmarket.analytics.export.PdfReportWriter;
import com.example.stockmarket.analytics.export.TextReportWriter;
import com.example.stockmarket.analytics.model.ComparisonReport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReportWritersTest {

    private final AnalyticsService service = new AnalyticsService(new DemoMarketDataSource(),
            Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC));

    private final ComparisonReport pair = service.compare("AAPL,TCS", "1day", 30);
    private final ComparisonReport single = service.compare("MSFT", "1day", 30);

    @Test
    void textReportContainsMetricsHistoryAndDemoLabel() {
        String txt = TextReportWriter.render(pair);

        assertThat(txt).startsWith("Stock Comparison: AAPL vs TCS\n");
        assertThat(txt).contains("KEY METRICS", "Period return", "Moving average", "Best performer:",
                "RECENT PRICES - AAPL (USD)", "RECENT PRICES - TCS (INR)", "DEMO DATA");
        assertThat(txt).contains("225.80");      // AAPL demo last price
        assertThat(txt).contains("4,120.50");    // TCS demo last price
        assertThat(txt.chars().allMatch(c -> c == '\n' || c >= 32 && c < 127)).as("plain ASCII").isTrue();
    }

    @Test
    void pdfHasSummaryAndPerStockPages() throws Exception {
        byte[] pdf = PdfReportWriter.write(pair);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(3); // summary + AAPL + TCS
            assertThat(doc.getDocumentInformation().getTitle()).isEqualTo("Stock Comparison: AAPL vs TCS");
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Key metrics", "Period return", "Recent prices (INR)", "Page 3 of 3", "DEMO DATA");
        }

        try (PDDocument doc = Loader.loadPDF(PdfReportWriter.write(single))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2); // summary + recent prices
        }
    }

    @Test
    void excelHasSummaryPerformanceChartAndHistorySheets() throws Exception {
        byte[] xlsx = ExcelReportWriter.write(pair);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(4);
            assertThat(wb.getSheetName(0)).isEqualTo("Summary");
            assertThat(wb.getSheetName(1)).isEqualTo("Performance");
            assertThat(wb.getSheetName(2)).isEqualTo("AAPL");
            assertThat(wb.getSheetName(3)).isEqualTo("TCS");

            // Metric values are stored as real numbers, not text.
            Sheet summary = wb.getSheet("Summary");
            boolean foundPrice = false;
            for (var row : summary) {
                if (row.getCell(0) != null && "Last price".equals(row.getCell(0).getStringCellValue())) {
                    assertThat(row.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
                    assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(225.80);
                    foundPrice = true;
                }
            }
            assertThat(foundPrice).isTrue();

            // Native chart exists with one series per stock.
            XSSFSheet perf = wb.getSheet("Performance");
            var charts = perf.getDrawingPatriarch().getCharts();
            assertThat(charts).hasSize(1);
            assertThat(charts.get(0).getChartSeries().get(0).getSeriesCount()).isEqualTo(2);

            // Formulas evaluate: first "% since start" is 0, return column matches closes.
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            var firstPct = perf.getRow(1).getCell(3);
            assertThat(eval.evaluate(firstPct).getNumberValue()).isCloseTo(0.0, within(1e-9));

            Sheet aapl = wb.getSheet("AAPL");
            assertThat(aapl.getLastRowNum()).isEqualTo(30);
            double c1 = aapl.getRow(1).getCell(4).getNumericCellValue();
            double c2 = aapl.getRow(2).getCell(4).getNumericCellValue();
            assertThat(eval.evaluate(aapl.getRow(2).getCell(7)).getNumberValue()).isCloseTo(c2 / c1 - 1, within(1e-9));
        }
    }

    @Test
    void chartRendersPng() throws Exception {
        byte[] png = ChartRenderer.renderPng(pair, 800, 400);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(img.getWidth()).isEqualTo(800);
        assertThat(img.getHeight()).isEqualTo(400);
        assertThat(ChartRenderer.renderPng(single, 300, 200)).isNotEmpty();
    }
}
