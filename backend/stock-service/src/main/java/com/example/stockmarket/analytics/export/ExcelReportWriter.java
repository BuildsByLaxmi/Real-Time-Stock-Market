package com.example.stockmarket.analytics.export;

import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.StockReport;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Excel workbook:
 * <ul>
 *   <li>"Summary" - highlights and the metric table (real numbers with Excel number formats)</li>
 *   <li>"Performance" - all stocks' closes on one date axis, % change since start, and a native Excel line chart</li>
 *   <li>one sheet per symbol - full OHLCV history, moving average, and a daily-return formula column</li>
 * </ul>
 */
public final class ExcelReportWriter {

    private final XSSFWorkbook wb = new XSSFWorkbook();
    private final CellStyle title;
    private final CellStyle muted;
    private final CellStyle header;
    private final CellStyle label;
    private final CellStyle text;
    private final CellStyle price;
    private final CellStyle percent;
    private final CellStyle volume;

    private ExcelReportWriter() {
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        title = wb.createCellStyle();
        title.setFont(titleFont);

        Font mutedFont = wb.createFont();
        mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        mutedFont.setFontHeightInPoints((short) 9);
        muted = wb.createCellStyle();
        muted.setFont(mutedFont);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        header = wb.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);

        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        label = bordered(wb.createCellStyle());
        label.setFont(boldFont);

        text = bordered(wb.createCellStyle());
        text.setAlignment(HorizontalAlignment.RIGHT);
        price = numeric("#,##0.00");
        percent = numeric("+0.00%;-0.00%;0.00%");
        volume = numeric("#,##0");
    }

    public static byte[] write(ComparisonReport report) {
        ExcelReportWriter writer = new ExcelReportWriter();
        try (XSSFWorkbook wb = writer.wb; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.summarySheet(report);
            writer.performanceSheet(report);
            for (StockReport s : report.stocks()) writer.historySheet(s);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build Excel report", e);
        }
    }

    private void summarySheet(ComparisonReport report) {
        Sheet sh = wb.createSheet("Summary");
        int r = 0;
        set(sh.createRow(r++), 0, ReportMetrics.title(report), title);
        set(sh.createRow(r++), 0, ReportMetrics.generatedLine(report), muted);
        set(sh.createRow(r++), 0, ReportMetrics.disclaimer(report), muted);
        r++;
        for (String line : ReportMetrics.highlights(report)) set(sh.createRow(r++), 0, line, null);
        r++;

        Row head = sh.createRow(r++);
        set(head, 0, "Metric", header);
        for (int c = 0; c < report.stocks().size(); c++) set(head, c + 1, report.stocks().get(c).symbol(), header);

        for (ReportMetrics.Metric m : ReportMetrics.METRICS) {
            Row row = sh.createRow(r++);
            set(row, 0, m.label(), label);
            for (int c = 0; c < report.stocks().size(); c++) {
                StockReport s = report.stocks().get(c);
                Double v = m.numeric().apply(s);
                Cell cell = row.createCell(c + 1);
                if (v == null) {
                    cell.setCellValue(m.kind() == ReportMetrics.Kind.TEXT ? m.text().apply(s) : "-");
                    cell.setCellStyle(text);
                } else {
                    cell.setCellValue(v);
                    cell.setCellStyle(switch (m.kind()) {
                        case PERCENT -> percent;
                        case VOLUME -> volume;
                        default -> price;
                    });
                }
            }
        }
        sh.setColumnWidth(0, 24 * 256);
        for (int c = 1; c <= report.stocks().size(); c++) sh.setColumnWidth(c, 26 * 256);
        sh.createFreezePane(1, 0);
    }

    private void performanceSheet(ComparisonReport report) {
        XSSFSheet sh = wb.createSheet("Performance");
        List<StockReport> stocks = report.stocks();
        int n = stocks.size();

        // Align every stock on one sorted date axis.
        TreeSet<String> dates = new TreeSet<>();
        List<Map<String, Double>> closes = new ArrayList<>();
        for (StockReport s : stocks) {
            Map<String, Double> m = new HashMap<>();
            s.history().forEach(p -> {
                dates.add(p.datetime());
                m.put(p.datetime(), p.close());
            });
            closes.add(m);
        }

        Row head = sh.createRow(0);
        set(head, 0, "Date", header);
        for (int i = 0; i < n; i++) {
            set(head, 1 + i, stocks.get(i).symbol() + " close", header);
            set(head, 1 + n + i, stocks.get(i).symbol() + " % since start", header);
        }

        int r = 1;
        for (String d : dates) {
            Row row = sh.createRow(r);
            set(row, 0, d, label);
            for (int i = 0; i < n; i++) {
                Double close = closes.get(i).get(d);
                if (close == null) continue;
                Cell c = row.createCell(1 + i);
                c.setCellValue(close);
                c.setCellStyle(price);
                // % change vs. this stock's first close in the window, as a live formula.
                String col = CellReference.convertNumToColString(1 + i);
                String firstRow = Integer.toString(firstRowOf(stocks.get(i), dates) + 1);
                Cell pct = row.createCell(1 + n + i);
                pct.setCellFormula(col + (r + 1) + "/" + col + "$" + firstRow + "-1");
                pct.setCellStyle(percent);
            }
            r++;
        }
        int lastRow = r - 1;

        sh.setColumnWidth(0, 20 * 256);
        for (int c = 1; c <= 2 * n; c++) sh.setColumnWidth(c, 18 * 256);
        sh.createFreezePane(1, 1);

        // Native Excel chart: one stock -> its close price; several -> % since start (comparable scales).
        boolean single = n == 1;
        XSSFDrawing drawing = sh.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 2 * n + 2, 1, 2 * n + 14, 24);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(single ? stocks.get(0).symbol() + " close price" : "Performance since start");
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        if (!single) yAxis.setNumberFormat("0%");

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, xAxis, yAxis);
        var categories = XDDFDataSourcesFactory.fromStringCellRange(sh, new CellRangeAddress(1, lastRow, 0, 0));
        for (int i = 0; i < n; i++) {
            int col = single ? 1 : 1 + n + i;
            var values = XDDFDataSourcesFactory.fromNumericCellRange(sh, new CellRangeAddress(1, lastRow, col, col));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(categories, values);
            series.setTitle(stocks.get(i).symbol(), null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.NONE);
        }
        chart.plot(data);
    }

    private void historySheet(StockReport s) {
        Sheet sh = wb.createSheet(sheetName(s.symbol()));
        String[] cols = {"Date", "Open", "High", "Low", "Close", "Volume",
                s.stats().movingAverageWindow() + "-bar moving avg", "Return vs previous"};
        Row head = sh.createRow(0);
        for (int c = 0; c < cols.length; c++) set(head, c, cols[c], header);

        List<PricePoint> h = s.history();
        for (int i = 0; i < h.size(); i++) {
            PricePoint p = h.get(i);
            Row row = sh.createRow(i + 1);
            set(row, 0, p.datetime(), label);
            num(row, 1, p.open(), price);
            num(row, 2, p.high(), price);
            num(row, 3, p.low(), price);
            num(row, 4, p.close(), price);
            num(row, 5, p.volume(), volume);
            Double ma = s.movingAverage().get(i);
            if (ma != null) num(row, 6, ma, price);
            if (i > 0) {
                Cell ret = row.createCell(7);
                ret.setCellFormula("E" + (i + 2) + "/E" + (i + 1) + "-1");
                ret.setCellStyle(percent);
            }
        }
        sh.setColumnWidth(0, 20 * 256);
        for (int c = 1; c < cols.length; c++) sh.setColumnWidth(c, 16 * 256);
        sh.createFreezePane(1, 1);
        sh.setAutoFilter(new CellRangeAddress(0, h.size(), 0, cols.length - 1));
    }

    // ---------------------------------------------------------------- helpers

    private static int firstRowOf(StockReport s, TreeSet<String> dates) {
        return dates.headSet(s.history().get(0).datetime()).size() + 1;
    }

    /** Excel sheet names: max 31 chars, no []:*?/\ and must not clash with "Summary"/"Performance". */
    private String sheetName(String symbol) {
        String name = symbol.replaceAll("[\\[\\]:*?/\\\\]", "_");
        if (name.equalsIgnoreCase("Summary") || name.equalsIgnoreCase("Performance")) name = name + " prices";
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    private CellStyle bordered(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle numeric(String format) {
        CellStyle style = bordered(wb.createCellStyle());
        style.setDataFormat(wb.createDataFormat().getFormat(format));
        return style;
    }

    private static void set(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private static void num(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
