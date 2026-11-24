package com.example.stockmarket.analytics;

import com.example.stockmarket.analytics.export.ChartRenderer;
import com.example.stockmarket.analytics.export.ExcelReportWriter;
import com.example.stockmarket.analytics.export.PdfReportWriter;
import com.example.stockmarket.analytics.export.ReportMetrics;
import com.example.stockmarket.analytics.export.TextReportWriter;
import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.ReportFormat;
import com.example.stockmarket.analytics.model.StockReport;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics & Reports (Member 4).
 * <pre>
 * GET /api/analytics/history?symbol=AAPL               price history + stats (price chart screen)
 * GET /api/analytics/compare?symbols=AAPL,MSFT         side-by-side comparison (compare screen)
 * GET /api/analytics/chart?symbols=AAPL,MSFT           PNG price chart
 * GET /api/reports/{text|pdf|excel}?symbols=AAPL,MSFT  downloadable report
 * </pre>
 * All accept optional {@code interval} (default 1day) and {@code outputsize} (default 30).
 */
@RestController
public class AnalyticsController {

    private static final String DEFAULT_INTERVAL = "1day";
    private static final String DEFAULT_SIZE = "30";

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/api/analytics/history")
    public StockReport history(@RequestParam String symbol,
                               @RequestParam(defaultValue = DEFAULT_INTERVAL) String interval,
                               @RequestParam(defaultValue = DEFAULT_SIZE) int outputsize) {
        return analytics.history(symbol, interval, outputsize);
    }

    @GetMapping("/api/analytics/compare")
    public ComparisonReport compare(@RequestParam String symbols,
                                    @RequestParam(defaultValue = DEFAULT_INTERVAL) String interval,
                                    @RequestParam(defaultValue = DEFAULT_SIZE) int outputsize) {
        return analytics.compare(symbols, interval, outputsize);
    }

    @GetMapping("/api/analytics/chart")
    public ResponseEntity<byte[]> chart(@RequestParam String symbols,
                                        @RequestParam(defaultValue = DEFAULT_INTERVAL) String interval,
                                        @RequestParam(defaultValue = DEFAULT_SIZE) int outputsize,
                                        @RequestParam(defaultValue = "900") int width,
                                        @RequestParam(defaultValue = "450") int height) {
        if (width < 300 || width > 2400 || height < 200 || height > 1600) {
            throw new IllegalArgumentException("width must be 300-2400 and height 200-1600");
        }
        ComparisonReport report = analytics.compare(symbols, interval, outputsize);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(ChartRenderer.renderPng(report, width, height));
    }

    @GetMapping("/api/reports/{format}")
    public ResponseEntity<byte[]> report(@PathVariable String format,
                                         @RequestParam String symbols,
                                         @RequestParam(defaultValue = DEFAULT_INTERVAL) String interval,
                                         @RequestParam(defaultValue = DEFAULT_SIZE) int outputsize,
                                         @RequestParam(defaultValue = "false") boolean inline) {
        ReportFormat f = ReportFormat.parse(format);
        ComparisonReport report = analytics.compare(symbols, interval, outputsize);
        byte[] body = switch (f) {
            case TEXT -> TextReportWriter.write(report);
            case PDF -> PdfReportWriter.write(report);
            case EXCEL -> ExcelReportWriter.write(report);
        };
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(ReportMetrics.fileName(report, f.extension()))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(f.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
