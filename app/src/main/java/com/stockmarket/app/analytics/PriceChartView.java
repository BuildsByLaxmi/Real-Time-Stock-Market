package com.stockmarket.app.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight line chart (no third-party library).
 * <ul>
 *   <li>{@link #showPrice(StockSeries)}: close price plus dashed moving average</li>
 *   <li>{@link #showPerformance(List)}: several stocks rebased to 0% at the first day, so different price levels compare fairly</li>
 * </ul>
 * Touch and drag to read the values for a given day.
 */
public class PriceChartView extends View {

    /** Series colours; the compare screen uses the same order for its legend. */
    public static final int[] PALETTE = {
            Color.parseColor("#2962FF"), Color.parseColor("#FF9800"),
            Color.parseColor("#26A69A"), Color.parseColor("#AB47BC")};

    private static final int GRID = Color.parseColor("#2A2E39");
    private static final int AXIS_TEXT = Color.parseColor("#787B86");

    private static class Line {
        String label;
        double[] values; // NaN = gap
        int color;
        boolean dashed;
    }

    private final List<Line> lines = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private boolean percentMode;
    private int touchIndex = -1;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF plot = new RectF();

    public PriceChartView(Context context) {
        this(context, null);
    }

    public PriceChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        gridPaint.setColor(GRID);
        gridPaint.setStrokeWidth(dp(1));
        textPaint.setColor(AXIS_TEXT);
        textPaint.setTextSize(sp(10));
        tipPaint.setColor(Color.parseColor("#E6131722"));
        setContentDescription("Price chart");
    }

    public void showPrice(StockSeries s) {
        lines.clear();
        percentMode = false;
        dates = s.dates;
        Line close = new Line();
        close.label = s.symbol;
        close.values = s.closes;
        close.color = PALETTE[0];
        lines.add(close);
        if (s.movingAverage != null) {
            Line ma = new Line();
            ma.label = s.movingAverageWindow + "d avg";
            ma.values = s.movingAverage;
            ma.color = PALETTE[1];
            ma.dashed = true;
            lines.add(ma);
        }
        touchIndex = -1;
        setContentDescription(String.format(Locale.US, "%s price chart, %d days, return %+.2f%%",
                s.symbol, s.closes.length, s.returnPercent));
        invalidate();
    }

    public void showPerformance(List<StockSeries> stocks) {
        lines.clear();
        percentMode = true;
        // Use the longest date axis; series are aligned on their most recent day.
        dates = new ArrayList<>();
        for (StockSeries s : stocks) if (s.dates.size() > dates.size()) dates = s.dates;
        StringBuilder desc = new StringBuilder("Performance comparison:");
        for (int k = 0; k < stocks.size(); k++) {
            StockSeries s = stocks.get(k);
            int offset = dates.size() - s.closes.length;
            double[] v = new double[dates.size()];
            for (int i = 0; i < v.length; i++) {
                int j = i - offset;
                v[i] = j < 0 ? Double.NaN : (s.closes[j] / s.closes[0] - 1) * 100;
            }
            Line l = new Line();
            l.label = s.symbol;
            l.values = v;
            l.color = PALETTE[k % PALETTE.length];
            lines.add(l);
            desc.append(String.format(Locale.US, " %s %+.2f%%;", s.symbol, s.returnPercent));
        }
        touchIndex = -1;
        setContentDescription(desc.toString());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = resolveSize((int) dp(220), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lines.isEmpty() || dates.isEmpty()) return;

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Line l : lines) {
            for (double v : l.values) {
                if (Double.isNaN(v)) continue;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        if (min == Double.MAX_VALUE) return;
        if (min == max) {
            min -= 1;
            max += 1;
        }
        double padRange = (max - min) * 0.08;
        min -= padRange;
        max += padRange;

        // Layout: y labels on the right (like trading apps), x labels at the bottom.
        float labelW = textPaint.measureText(format(max)) + dp(8);
        plot.set(dp(4), dp(8), getWidth() - labelW, getHeight() - dp(22));

        // Grid + y labels.
        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            double v = min + (max - min) * i / ticks;
            float y = yFor(v, min, max);
            canvas.drawLine(plot.left, y, plot.right, y, gridPaint);
            canvas.drawText(format(v), plot.right + dp(6), y + textPaint.getTextSize() / 3, textPaint);
        }
        if (percentMode && min < 0 && max > 0) {
            gridPaint.setColor(AXIS_TEXT);
            float y0 = yFor(0, min, max);
            canvas.drawLine(plot.left, y0, plot.right, y0, gridPaint);
            gridPaint.setColor(GRID);
        }

        // X labels: first, middle, last.
        int n = dates.size();
        float baseline = getHeight() - dp(6);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(shortDate(dates.get(0)), plot.left, baseline, textPaint);
        if (n > 2) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(shortDate(dates.get(n / 2)), xFor(n / 2, n), baseline, textPaint);
        }
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(shortDate(dates.get(n - 1)), plot.right, baseline, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Area under the first line (single-stock view only).
        if (!percentMode) {
            Line first = lines.get(0);
            path.reset();
            boolean started = false;
            int lastI = 0;
            for (int i = 0; i < n; i++) {
                if (Double.isNaN(first.values[i])) continue;
                float x = xFor(i, n), y = yFor(first.values[i], min, max);
                if (!started) {
                    path.moveTo(x, plot.bottom);
                    path.lineTo(x, y);
                    started = true;
                } else {
                    path.lineTo(x, y);
                }
                lastI = i;
            }
            path.lineTo(xFor(lastI, n), plot.bottom);
            path.close();
            fillPaint.setColor((first.color & 0x00FFFFFF) | 0x22000000);
            canvas.drawPath(path, fillPaint);
        }

        // Lines.
        for (Line l : lines) {
            path.reset();
            boolean pen = false;
            for (int i = 0; i < n && i < l.values.length; i++) {
                if (Double.isNaN(l.values[i])) {
                    pen = false;
                    continue;
                }
                float x = xFor(i, n), y = yFor(l.values[i], min, max);
                if (pen) path.lineTo(x, y);
                else path.moveTo(x, y);
                pen = true;
            }
            linePaint.setColor(l.color);
            linePaint.setPathEffect(l.dashed ? new DashPathEffect(new float[]{dp(5), dp(4)}, 0) : null);
            linePaint.setStrokeWidth(l.dashed ? dp(1.5f) : dp(2));
            canvas.drawPath(path, linePaint);
        }
        linePaint.setPathEffect(null);

        if (touchIndex >= 0 && touchIndex < n) drawTooltip(canvas, touchIndex, n, min, max);
    }

    private void drawTooltip(Canvas canvas, int index, int n, double min, double max) {
        float x = xFor(index, n);
        gridPaint.setColor(AXIS_TEXT);
        canvas.drawLine(x, plot.top, x, plot.bottom, gridPaint);
        gridPaint.setColor(GRID);

        List<String> rows = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        rows.add(dates.get(index));
        colors.add(Color.WHITE);
        for (Line l : lines) {
            if (index >= l.values.length || Double.isNaN(l.values[index])) continue;
            fillPaint.setColor(l.color);
            canvas.drawCircle(x, yFor(l.values[index], min, max), dp(3.5f), fillPaint);
            rows.add(l.label + "  " + format(l.values[index]));
            colors.add(l.color);
        }

        float lineH = textPaint.getTextSize() * 1.4f;
        float w = 0;
        for (String r : rows) w = Math.max(w, textPaint.measureText(r));
        w += dp(16);
        float h = rows.size() * lineH + dp(8);
        float left = x + dp(8) + w > plot.right ? x - dp(8) - w : x + dp(8);
        RectF box = new RectF(left, plot.top, left + w, plot.top + h);
        canvas.drawRoundRect(box, dp(6), dp(6), tipPaint);
        int old = textPaint.getColor();
        for (int i = 0; i < rows.size(); i++) {
            textPaint.setColor(colors.get(i));
            canvas.drawText(rows.get(i), box.left + dp(8), box.top + dp(4) + lineH * (i + 1) - lineH * 0.3f, textPaint);
        }
        textPaint.setColor(old);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (dates.isEmpty() || plot.width() <= 0) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true); // don't let the ScrollView steal the drag
                int n = dates.size();
                float t = (e.getX() - plot.left) / plot.width();
                touchIndex = Math.max(0, Math.min(n - 1, Math.round(t * (n - 1))));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                touchIndex = -1;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private float xFor(int i, int n) {
        return n <= 1 ? plot.left : plot.left + plot.width() * i / (n - 1);
    }

    private float yFor(double v, double min, double max) {
        return (float) (plot.bottom - (v - min) / (max - min) * plot.height());
    }

    private String format(double v) {
        if (percentMode) return String.format(Locale.US, "%+.1f%%", v);
        return Math.abs(v) >= 1000 ? String.format(Locale.US, "%,.0f", v) : String.format(Locale.US, "%.2f", v);
    }

    private static String shortDate(String d) {
        // "2026-11-24" -> "24 Nov"
        if (d == null || d.length() < 10) return d == null ? "" : d;
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        try {
            int m = Integer.parseInt(d.substring(5, 7));
            return Integer.parseInt(d.substring(8, 10)) + " " + months[m - 1];
        } catch (RuntimeException e) {
            return d.substring(0, 10);
        }
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
