package com.stockmarket.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class CompareActivity extends AppCompatActivity {

    public static class CompareStock {
        public String symbol;
        public String exchange;
        public String company;
        public String price;
        public String change;
        public boolean isPositive;
        public String marketCap;
        public String peRatio;
        public String high52;
        public String volume;

        public CompareStock(String symbol, String exchange, String company, String price,
                            String change, boolean isPositive, String marketCap, String peRatio,
                            String high52, String volume) {
            this.symbol = symbol;
            this.exchange = exchange;
            this.company = company;
            this.price = price;
            this.change = change;
            this.isPositive = isPositive;
            this.marketCap = marketCap;
            this.peRatio = peRatio;
            this.high52 = high52;
            this.volume = volume;
        }
    }

    private final Map<String, CompareStock> stockMap = new HashMap<>();

    private Spinner spStockA;
    private Spinner spStockB;
    private Button btnBackCompare;

    private TextView tvCompareSymbolA, tvCompareCompanyA, tvComparePriceA, tvCompareChangeA;
    private TextView tvCompareSymbolB, tvCompareCompanyB, tvComparePriceB, tvCompareChangeB;

    private TextView tvValMarketCapA, tvValMarketCapB;
    private TextView tvValPeA, tvValPeB;
    private TextView tvValHigh52A, tvValHigh52B;
    private TextView tvValVolumeA, tvValVolumeB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        initData();
        initViews();
        setupSpinners();

        btnBackCompare.setOnClickListener(v -> finish());
    }

    private void initData() {
        stockMap.put("TCS", new CompareStock("TCS", "NSE", "Tata Consultancy Services", "₹4,120.50", "+3.99% ▲", true, "₹15.2 Trillion", "31.4", "₹4,250.00", "2.4M"));
        stockMap.put("INFY", new CompareStock("INFY", "NSE", "Infosys Limited", "₹1,880.20", "-1.18% ▼", false, "₹7.8 Trillion", "27.2", "₹1,990.00", "5.1M"));
        stockMap.put("RELIANCE", new CompareStock("RELIANCE", "NSE", "Reliance Industries Ltd", "₹2,950.00", "+1.55% ▲", true, "₹19.9 Trillion", "26.5", "₹3,217.00", "6.8M"));
        stockMap.put("AAPL", new CompareStock("AAPL", "NASDAQ", "Apple Inc.", "$225.80", "+5.22% ▲", true, "$3.45 Trillion", "33.8", "$237.23", "48.5M"));
        stockMap.put("MSFT", new CompareStock("MSFT", "NASDAQ", "Microsoft Corporation", "$428.15", "+1.85% ▲", true, "$3.18 Trillion", "35.1", "$468.35", "22.3M"));
        stockMap.put("GOOGL", new CompareStock("GOOGL", "NASDAQ", "Alphabet Inc.", "$178.60", "+2.30% ▲", true, "$2.20 Trillion", "24.3", "$191.75", "19.8M"));
        stockMap.put("TSLA", new CompareStock("TSLA", "NASDAQ", "Tesla, Inc.", "$328.50", "+8.14% ▲", true, "$1.05 Trillion", "88.4", "$358.64", "65.4M"));
        stockMap.put("WIPRO", new CompareStock("WIPRO", "NSE", "Wipro Limited", "₹540.10", "-2.04% ▼", false, "₹2.8 Trillion", "22.8", "₹580.00", "3.9M"));
    }

    private void initViews() {
        spStockA = findViewById(R.id.spStockA);
        spStockB = findViewById(R.id.spStockB);
        btnBackCompare = findViewById(R.id.btnBackCompare);

        tvCompareSymbolA = findViewById(R.id.tvCompareSymbolA);
        tvCompareCompanyA = findViewById(R.id.tvCompareCompanyA);
        tvComparePriceA = findViewById(R.id.tvComparePriceA);
        tvCompareChangeA = findViewById(R.id.tvCompareChangeA);

        tvCompareSymbolB = findViewById(R.id.tvCompareSymbolB);
        tvCompareCompanyB = findViewById(R.id.tvCompareCompanyB);
        tvComparePriceB = findViewById(R.id.tvComparePriceB);
        tvCompareChangeB = findViewById(R.id.tvCompareChangeB);

        tvValMarketCapA = findViewById(R.id.tvValMarketCapA);
        tvValMarketCapB = findViewById(R.id.tvValMarketCapB);
        tvValPeA = findViewById(R.id.tvValPeA);
        tvValPeB = findViewById(R.id.tvValPeB);
        tvValHigh52A = findViewById(R.id.tvValHigh52A);
        tvValHigh52B = findViewById(R.id.tvValHigh52B);
        tvValVolumeA = findViewById(R.id.tvValVolumeA);
        tvValVolumeB = findViewById(R.id.tvValVolumeB);
    }

    private void setupSpinners() {
        String[] tickers = stockMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tickers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spStockA.setAdapter(adapter);
        spStockB.setAdapter(adapter);

        // Default selections: TCS vs INFY
        spStockA.setSelection(0);
        spStockB.setSelection(Math.min(1, tickers.length - 1));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateComparison();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spStockA.setOnItemSelectedListener(listener);
        spStockB.setOnItemSelectedListener(listener);

        updateComparison();
    }

    private void updateComparison() {
        String keyA = spStockA.getSelectedItem() != null ? spStockA.getSelectedItem().toString() : "TCS";
        String keyB = spStockB.getSelectedItem() != null ? spStockB.getSelectedItem().toString() : "INFY";

        CompareStock stockA = stockMap.get(keyA);
        CompareStock stockB = stockMap.get(keyB);

        if (stockA != null) {
            tvCompareSymbolA.setText(stockA.symbol + " (" + stockA.exchange + ")");
            tvCompareCompanyA.setText(stockA.company);
            tvComparePriceA.setText(stockA.price);
            tvCompareChangeA.setText(stockA.change);
            tvCompareChangeA.setTextColor(stockA.isPositive ? Color.parseColor("#26A69A") : Color.parseColor("#EF5350"));

            tvValMarketCapA.setText(stockA.marketCap);
            tvValPeA.setText(stockA.peRatio);
            tvValHigh52A.setText(stockA.high52);
            tvValVolumeA.setText(stockA.volume);
        }

        if (stockB != null) {
            tvCompareSymbolB.setText(stockB.symbol + " (" + stockB.exchange + ")");
            tvCompareCompanyB.setText(stockB.company);
            tvComparePriceB.setText(stockB.price);
            tvCompareChangeB.setText(stockB.change);
            tvCompareChangeB.setTextColor(stockB.isPositive ? Color.parseColor("#26A69A") : Color.parseColor("#EF5350"));

            tvValMarketCapB.setText(stockB.marketCap);
            tvValPeB.setText(stockB.peRatio);
            tvValHigh52B.setText(stockB.high52);
            tvValVolumeB.setText(stockB.volume);
        }
    }
}
