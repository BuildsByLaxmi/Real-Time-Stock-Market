package com.stockmarket.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.stockmarket.app.analytics.AnalyticsApi;
import com.stockmarket.app.analytics.Comparison;
import com.stockmarket.app.analytics.PriceChartView;
import com.stockmarket.app.analytics.ReportActions;
import com.stockmarket.app.analytics.StockSeries;
import com.stockmarket.app.analytics.TextReport;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Price chart with range selector, period analytics and PDF / Excel / Text report downloads for one stock. */
public class ChartActivity extends AppCompatActivity {

    public static final String EXTRA_SYMBOL = "symbol";

    private static final int GAIN = Color.parseColor("#26A69A");
    private static final int LOSS = Color.parseColor("#EF5350");

    private String symbol;
    private int bars = 22; // ~1 month of trading days
    private int requestId;
    private Comparison current;

    private AnalyticsApi api;
    private ReportActions reports;

    private PriceChartView chart;
    private ProgressBar progress;
    private TextView tvSymbol, tvCompany, tvPrice, tvReturn, tvSource;
    private LinearLayout statsContainer;
    private Button btnPdf, btnExcel, btnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        symbol = getIntent().getStringExtra(EXTRA_SYMBOL);
        if (symbol == null || symbol.isEmpty()) symbol = "AAPL";
        if (savedInstanceState != null) bars = savedInstanceState.getInt("bars", bars);

        api = new AnalyticsApi(this);
        reports = new ReportActions(this, api);

        chart = findViewById(R.id.priceChart);
        progress = findViewById(R.id.chartProgress);
        tvSymbol = findViewById(R.id.tvChartSymbol);
        tvCompany = findViewById(R.id.tvChartCompany);
        tvPrice = findViewById(R.id.tvChartPrice);
        tvReturn = findViewById(R.id.tvChartReturn);
        tvSource = findViewById(R.id.tvChartSource);
        statsContainer = findViewById(R.id.statsContainer);
        btnPdf = findViewById(R.id.btnReportPdf);
        btnExcel = findViewById(R.id.btnReportExcel);
        btnText = findViewById(R.id.btnReportText);

        tvSymbol.setText(symbol);
        findViewById(R.id.btnBackChart).setOnClickListener(v -> finish());

        MaterialButtonToggleGroup range = findViewById(R.id.rangeGroup);
        range.check(idForBars(bars));
        range.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            bars = barsFor(checkedId);
            load();
        });

        List<String> symbols = Collections.singletonList(symbol);
        btnPdf.setOnClickListener(v -> reports.export(ReportActions.PDF, symbols, bars, current, btnPdf, btnExcel, btnText));
        btnExcel.setOnClickListener(v -> reports.export(ReportActions.EXCEL, symbols, bars, current, btnPdf, btnExcel, btnText));
        btnText.setOnClickListener(v -> reports.export(ReportActions.TEXT, symbols, bars, current, btnPdf, btnExcel, btnText));

        load();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("bars", bars);
    }

    private void load() {
        int id = ++requestId; // ignore responses for a range the user has already left
        progress.setVisibility(View.VISIBLE);
        api.compare(Collections.singletonList(symbol), bars, new AnalyticsApi.Callback<Comparison>() {
            @Override
            public void onResult(Comparison result) {
                if (id != requestId || isFinishing()) return;
                progress.setVisibility(View.GONE);
                current = result;
                show(result.stocks.get(0), result);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
            }
        });
    }

    private void show(StockSeries s, Comparison c) {
        tvSymbol.setText(s.exchange.isEmpty() ? s.symbol : s.symbol + " (" + s.exchange + ")");
        tvCompany.setText(s.name);
        tvPrice.setText(s.currency + " " + TextReport.money(s.price));
        tvReturn.setText(String.format(Locale.US, "%s %s", TextReport.pct(s.returnPercent), rangeLabel()));
        tvReturn.setTextColor(s.returnPercent >= 0 ? GAIN : LOSS);
        tvSource.setText(c.sourceLabel() + " · touch the chart to read prices");
        tvSource.setTextColor(c.isLive() ? Color.parseColor("#787B86") : Color.parseColor("#FFCA28"));
        chart.showPrice(s);

        statsContainer.removeViews(1, statsContainer.getChildCount() - 1);
        stat("Period return", TextReport.pct(s.returnPercent), s.returnPercent >= 0 ? GAIN : LOSS);
        stat("Period high", TextReport.money(s.high), Color.WHITE);
        stat("Period low", TextReport.money(s.low), Color.WHITE);
        stat("Average close", TextReport.money(s.averageClose), Color.WHITE);
        stat("Daily volatility", String.format(Locale.US, "%.2f%%", s.volatilityPercent), Color.WHITE);
        stat(s.movingAverageWindow + "-day moving average", TextReport.money(s.movingAverageValue), Color.WHITE);
        boolean up = "UPTREND".equals(s.trend);
        stat("Trend", up ? "Above average ▲" : "Below average ▼", up ? GAIN : LOSS);
    }

    private void stat(String label, String value, int valueColor) {
        View row = getLayoutInflater().inflate(R.layout.item_stat_row, statsContainer, false);
        ((TextView) row.findViewById(R.id.tvStatLabel)).setText(label);
        TextView v = row.findViewById(R.id.tvStatValue);
        v.setText(value);
        v.setTextColor(valueColor);
        statsContainer.addView(row);
    }

    private String rangeLabel() {
        switch (bars) {
            case 5: return "past week";
            case 22: return "past month";
            case 66: return "past 3 months";
            case 130: return "past 6 months";
            default: return "past year";
        }
    }

    private static int barsFor(int buttonId) {
        if (buttonId == R.id.range1W) return 5;
        if (buttonId == R.id.range3M) return 66;
        if (buttonId == R.id.range6M) return 130;
        if (buttonId == R.id.range1Y) return 252;
        return 22;
    }

    private static int idForBars(int bars) {
        switch (bars) {
            case 5: return R.id.range1W;
            case 66: return R.id.range3M;
            case 130: return R.id.range6M;
            case 252: return R.id.range1Y;
            default: return R.id.range1M;
        }
    }
}
