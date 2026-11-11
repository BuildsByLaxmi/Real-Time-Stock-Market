package com.stockmarket.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportGenerator {

    public static File generateStockReport(Context context, MainActivity.StockItem stock, String userFullName) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 Size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();

        // 1. Header Banner
        paint.setColor(Color.parseColor("#131722"));
        canvas.drawRect(0, 0, 595, 90, paint);

        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("📈 StockPro - Equity Research Report", 40, 50, titlePaint);

        paint.setColor(Color.parseColor("#787B86"));
        paint.setTextSize(11);
        String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + dateStr + " | Analyst: " + userFullName, 40, 72, paint);

        // 2. Company & Ticker Overview
        int y = 130;
        headerPaint.setColor(Color.parseColor("#2962FF"));
        headerPaint.setTextSize(16);
        headerPaint.setFakeBoldText(true);
        canvas.drawText(stock.symbol + " (" + stock.exchange + ") - " + stock.companyName, 40, y, headerPaint);

        y += 25;
        paint.setColor(Color.BLACK);
        paint.setTextSize(13);
        canvas.drawText("Current Market Price: " + stock.price, 40, y, paint);

        paint.setColor(stock.isPositive ? Color.parseColor("#26A69A") : Color.parseColor("#EF5350"));
        paint.setFakeBoldText(true);
        canvas.drawText("24h Change: " + stock.change, 280, y, paint);

        // 3. Technical & Valuation Metrics Box
        y += 35;
        paint.setColor(Color.parseColor("#F4F5F7"));
        canvas.drawRoundRect(40, y, 555, y + 140, 10, 10, paint);

        paint.setColor(Color.parseColor("#1E222D"));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Fundamental & Performance Indicators", 60, y + 30, paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(12);
        canvas.drawText("• " + stock.dayHigh, 60, y + 60, paint);
        canvas.drawText("• " + stock.dayLow, 60, y + 85, paint);
        canvas.drawText("• " + stock.high52, 300, y + 60, paint);
        canvas.drawText("• " + stock.peRatio, 300, y + 85, paint);
        canvas.drawText("• Market Status: OPEN (Benchmark NSE/BSE)", 60, y + 115, paint);

        // 4. Benchmark Indices
        y += 180;
        headerPaint.setColor(Color.parseColor("#1E222D"));
        headerPaint.setTextSize(14);
        headerPaint.setFakeBoldText(true);
        canvas.drawText("Market Benchmark Performance", 40, y, headerPaint);

        y += 20;
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12);
        canvas.drawText("NIFTY 50:  24,140.20 (+0.45% ▲)", 40, y, paint);
        canvas.drawText("SENSEX:    79,480.10 (+0.52% ▲)", 300, y, paint);

        // 5. Disclaimer / Footer
        y = 780;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(40, y - 20, 555, y - 20, paint);
        paint.setColor(Color.GRAY);
        paint.setTextSize(9);
        canvas.drawText("Disclaimer: This report is generated automatically by Real-Time Stock Market Platform for academic demo purposes.", 40, y, paint);
        canvas.drawText("All financial metrics are simulated / sourced from public market feeds.", 40, y + 14, paint);

        document.finishPage(page);

        // Save PDF to cache dir
        File reportFile = new File(context.getCacheDir(), "Stock_Report_" + stock.symbol + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(reportFile)) {
            document.writeTo(fos);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            document.close();
        }

        return reportFile;
    }

    public static void openPdf(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Report file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(intent, "Open Stock Report with..."));
        } catch (Exception e) {
            // Fallback: share file
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share Stock Report via..."));
        }
    }
}
