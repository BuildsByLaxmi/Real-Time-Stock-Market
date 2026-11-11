package com.stockmarket.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.stockmarket.app.auth.SessionManager;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static class StockItem {
        public String symbol;
        public String exchange;
        public String companyName;
        public String price;
        public double rawPrice;
        public String change;
        public boolean isPositive;
        public String dayHigh;
        public String dayLow;
        public String high52;
        public String peRatio;

        public StockItem(String symbol, String exchange, String companyName, String price,
                         double rawPrice, String change, boolean isPositive, String dayHigh,
                         String dayLow, String high52, String peRatio) {
            this.symbol = symbol;
            this.exchange = exchange;
            this.companyName = companyName;
            this.price = price;
            this.rawPrice = rawPrice;
            this.change = change;
            this.isPositive = isPositive;
            this.dayHigh = dayHigh;
            this.dayLow = dayLow;
            this.high52 = high52;
            this.peRatio = peRatio;
        }
    }

    private TextView tvUserGreeting;
    private Button btnLogout;
    private TextView tvAutoRefreshStatus;
    private TextView btnToggleRefresh;
    private TextView tvNiftyPrice;
    private TextView tvSensexPrice;

    private EditText etSearchStock;
    private Button btnSearch;
    private TextView tvStockSymbol;
    private TextView tvCompanyName;
    private TextView tvStockPrice;
    private TextView tvStockChange;
    private TextView tvDayHigh;
    private TextView tvDayLow;
    private TextView tv52High;
    private TextView tvPeRatio;
    private Button btnAddToWatchlist;
    private Button btnOpenWebChart;
    private Button btnOpenCompare;
    private Button btnExportPdf;
    private LinearLayout llWatchlistContainer;

    private final Map<String, StockItem> stockDatabase = new HashMap<>();
    private final Set<String> watchlistSymbols = new HashSet<>();
    private StockItem currentStock;
    private SessionManager sessionManager;

    // 20-second Auto-Refresh Handler
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private boolean isAutoRefreshActive = true;
    private final Random random = new Random();

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoRefreshActive) {
                simulateLivePriceTick();
                autoRefreshHandler.postDelayed(this, 20000); // 20 Seconds
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // System Bar Insets
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initDatabase();
        initViews();
        setupListeners();

        // Default display TCS
        StockItem defaultStock = stockDatabase.get("TCS");
        if (defaultStock != null) {
            displayStock(defaultStock);
        }

        // Start 20s auto-refresh
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 20000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private void initDatabase() {
        stockDatabase.put("TCS", new StockItem("TCS", "NSE", "Tata Consultancy Services Ltd", "₹4,120.50", 4120.50, "+3.99% ▲", true, "Day High: ₹4,150.00", "Day Low: ₹3,970.00", "52W High: ₹4,250.00", "P/E Ratio: 31.4"));
        stockDatabase.put("AAPL", new StockItem("AAPL", "NASDAQ", "Apple Inc.", "$225.80", 225.80, "+5.22% ▲", true, "Day High: $227.00", "Day Low: $214.50", "52W High: $237.23", "P/E Ratio: 33.8"));
        stockDatabase.put("INFY", new StockItem("INFY", "NSE", "Infosys Limited", "₹1,880.20", 1880.20, "-1.18% ▼", false, "Day High: ₹1,910.00", "Day Low: ₹1,875.00", "52W High: ₹1,990.00", "P/E Ratio: 27.2"));
        stockDatabase.put("RELIANCE", new StockItem("RELIANCE", "NSE", "Reliance Industries Ltd", "₹2,950.00", 2950.00, "+1.55% ▲", true, "Day High: ₹2,975.00", "Day Low: ₹2,910.00", "52W High: ₹3,217.00", "P/E Ratio: 26.5"));
        stockDatabase.put("MSFT", new StockItem("MSFT", "NASDAQ", "Microsoft Corporation", "$428.15", 428.15, "+1.85% ▲", true, "Day High: $431.00", "Day Low: $422.50", "52W High: $468.35", "P/E Ratio: 35.1"));
        stockDatabase.put("GOOGL", new StockItem("GOOGL", "NASDAQ", "Alphabet Inc.", "$178.60", 178.60, "+2.30% ▲", true, "Day High: $180.20", "Day Low: $174.10", "52W High: $191.75", "P/E Ratio: 24.3"));
        stockDatabase.put("TSLA", new StockItem("TSLA", "NASDAQ", "Tesla, Inc.", "$328.50", 328.50, "+8.14% ▲", true, "Day High: $335.00", "Day Low: $305.20", "52W High: $358.64", "P/E Ratio: 88.4"));
        stockDatabase.put("META", new StockItem("META", "NASDAQ", "Meta Platforms Inc.", "$565.40", 565.40, "-3.02% ▼", false, "Day High: $582.00", "Day Low: $560.10", "52W High: $602.95", "P/E Ratio: 28.6"));
        stockDatabase.put("WIPRO", new StockItem("WIPRO", "NSE", "Wipro Limited", "₹540.10", 540.10, "-2.04% ▼", false, "Day High: ₹552.00", "Day Low: ₹538.50", "52W High: ₹580.00", "P/E Ratio: 22.8"));
        stockDatabase.put("HDFCBANK", new StockItem("HDFCBANK", "NSE", "HDFC Bank Ltd", "₹1,745.00", 1745.00, "+0.85% ▲", true, "Day High: ₹1,760.00", "Day Low: ₹1,732.00", "52W High: ₹1,794.00", "P/E Ratio: 19.4"));
        stockDatabase.put("TATAMOTORS", new StockItem("TATAMOTORS", "NSE", "Tata Motors Limited", "₹980.40", 980.40, "+2.15% ▲", true, "Day High: ₹995.00", "Day Low: ₹962.00", "52W High: ₹1,179.00", "P/E Ratio: 10.2"));

        watchlistSymbols.add("INFY");
        watchlistSymbols.add("RELIANCE");
    }

    private void initViews() {
        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        btnLogout = findViewById(R.id.btnLogout);
        tvAutoRefreshStatus = findViewById(R.id.tvAutoRefreshStatus);
        btnToggleRefresh = findViewById(R.id.btnToggleRefresh);
        tvNiftyPrice = findViewById(R.id.tvNiftyPrice);
        tvSensexPrice = findViewById(R.id.tvSensexPrice);

        etSearchStock = findViewById(R.id.etSearchStock);
        btnSearch = findViewById(R.id.btnSearch);
        tvStockSymbol = findViewById(R.id.tvStockSymbol);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvStockPrice = findViewById(R.id.tvStockPrice);
        tvStockChange = findViewById(R.id.tvStockChange);
        tvDayHigh = findViewById(R.id.tvDayHigh);
        tvDayLow = findViewById(R.id.tvDayLow);
        tv52High = findViewById(R.id.tv52High);
        tvPeRatio = findViewById(R.id.tvPeRatio);
        btnAddToWatchlist = findViewById(R.id.btnAddToWatchlist);
        btnOpenWebChart = findViewById(R.id.btnOpenWebChart);
        btnOpenCompare = findViewById(R.id.btnOpenCompare);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        llWatchlistContainer = findViewById(R.id.llWatchlistContainer);

        // Display user name from Session
        if (tvUserGreeting != null && sessionManager != null) {
            tvUserGreeting.setText("Welcome back, " + sessionManager.getFullName() + "! 👋");
        }
    }

    private void setupListeners() {
        // Logout Button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.logout();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Toggle Auto-Refresh
        if (btnToggleRefresh != null) {
            btnToggleRefresh.setOnClickListener(v -> {
                isAutoRefreshActive = !isAutoRefreshActive;
                if (isAutoRefreshActive) {
                    tvAutoRefreshStatus.setText("⏱️ Auto-Refresh: ON (Every 20s)");
                    tvAutoRefreshStatus.setTextColor(Color.parseColor("#26A69A"));
                    autoRefreshHandler.postDelayed(autoRefreshRunnable, 20000);
                    Toast.makeText(this, "Live 20s price polling resumed", Toast.LENGTH_SHORT).show();
                } else {
                    tvAutoRefreshStatus.setText("⏱️ Auto-Refresh: PAUSED");
                    tvAutoRefreshStatus.setTextColor(Color.parseColor("#EF5350"));
                    autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
                    Toast.makeText(this, "Price polling paused", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Search Button Action
        btnSearch.setOnClickListener(v -> {
            String query = etSearchStock.getText().toString().trim().toUpperCase();
            if (!query.isEmpty()) {
                searchStock(query);
            } else {
                Toast.makeText(this, "Please enter a stock symbol (e.g. TCS, AAPL, INFY)", Toast.LENGTH_SHORT).show();
            }
        });

        // Add to Watchlist Button Action
        btnAddToWatchlist.setOnClickListener(v -> {
            if (currentStock == null) return;
            if (watchlistSymbols.contains(currentStock.symbol)) {
                Toast.makeText(this, currentStock.symbol + " is already in your Watchlist!", Toast.LENGTH_SHORT).show();
                return;
            }
            addStockToWatchlistView(currentStock);
            watchlistSymbols.add(currentStock.symbol);
            Toast.makeText(this, "⭐ " + currentStock.symbol + " added to your Watchlist!", Toast.LENGTH_SHORT).show();
        });

        // Open Live Web Chart (Google Finance)
        btnOpenWebChart.setOnClickListener(v -> {
            if (currentStock == null) return;
            String webUrl = "https://www.google.com/finance/quote/" + currentStock.symbol + ":" + currentStock.exchange;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Open Comparison Activity
        if (btnOpenCompare != null) {
            btnOpenCompare.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompareActivity.class);
                startActivity(intent);
            });
        }

        // Export PDF Report
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                if (currentStock == null) return;
                Toast.makeText(this, "Generating PDF Report for " + currentStock.symbol + "...", Toast.LENGTH_SHORT).show();
                File pdf = ReportGenerator.generateStockReport(this, currentStock, sessionManager.getFullName());
                if (pdf != null) {
                    ReportGenerator.openPdf(this, pdf);
                } else {
                    Toast.makeText(this, "Failed to generate PDF report", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Top Movers Clicks
        setupTopMoverClick(R.id.tvMoverAapl, "AAPL");
        setupTopMoverClick(R.id.tvMoverTcs, "TCS");
        setupTopMoverClick(R.id.tvMoverReliance, "RELIANCE");
        setupTopMoverClick(R.id.tvMoverMeta, "META");
        setupTopMoverClick(R.id.tvMoverWipro, "WIPRO");
        setupTopMoverClick(R.id.tvMoverInfy, "INFY");

        // Initial Watchlist rows
        View rowInfy = findViewById(R.id.rowWatchlistInfy);
        if (rowInfy != null) rowInfy.setOnClickListener(v -> searchStock("INFY"));

        View rowRel = findViewById(R.id.rowWatchlistReliance);
        if (rowRel != null) rowRel.setOnClickListener(v -> searchStock("RELIANCE"));
    }

    private void setupTopMoverClick(int viewId, String ticker) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> searchStock(ticker));
        }
    }

    public void searchStock(String symbol) {
        StockItem item = stockDatabase.get(symbol);
        if (item == null) {
            item = new StockItem(
                    symbol,
                    "NSE",
                    symbol + " Enterprises Ltd",
                    "₹1,250.00",
                    1250.00,
                    "+2.10% ▲",
                    true,
                    "Day High: ₹1,275.00",
                    "Day Low: ₹1,230.00",
                    "52W High: ₹1,480.00",
                    "P/E Ratio: 25.0"
            );
            stockDatabase.put(symbol, item);
        }
        displayStock(item);
        Toast.makeText(this, "Loaded live quote for " + item.symbol, Toast.LENGTH_SHORT).show();
    }

    private void displayStock(StockItem stock) {
        this.currentStock = stock;
        tvStockSymbol.setText(stock.symbol + " (" + stock.exchange + ")");
        tvCompanyName.setText(stock.companyName);
        tvStockPrice.setText(stock.price);
        tvStockChange.setText(stock.change);

        if (stock.isPositive) {
            tvStockChange.setTextColor(Color.parseColor("#26A69A"));
            tvStockChange.setBackgroundColor(Color.parseColor("#1E382B"));
        } else {
            tvStockChange.setTextColor(Color.parseColor("#EF5350"));
            tvStockChange.setBackgroundColor(Color.parseColor("#3A1E22"));
        }

        if (tvDayHigh != null) tvDayHigh.setText(stock.dayHigh);
        if (tvDayLow != null) tvDayLow.setText(stock.dayLow);
        if (tv52High != null) tv52High.setText(stock.high52);
        if (tvPeRatio != null) tvPeRatio.setText(stock.peRatio);
    }

    private void simulateLivePriceTick() {
        if (currentStock == null) return;
        // Fluctuates +/- 0.3%
        double pctChange = (random.nextDouble() * 0.6) - 0.3;
        double newPrice = currentStock.rawPrice * (1.0 + (pctChange / 100.0));
        currentStock.rawPrice = Math.round(newPrice * 100.0) / 100.0;

        String currencySymbol = currentStock.exchange.equals("NASDAQ") ? "$" : "₹";
        currentStock.price = currencySymbol + String.format(java.util.Locale.US, "%,.2f", currentStock.rawPrice);

        double totalPct = Math.round(pctChange * 10.0) / 10.0;
        if (totalPct >= 0) {
            currentStock.change = "+" + String.format(java.util.Locale.US, "%.2f", Math.abs(totalPct) + 1.2) + "% ▲";
            currentStock.isPositive = true;
        } else {
            currentStock.change = "-" + String.format(java.util.Locale.US, "%.2f", Math.abs(totalPct)) + "% ▼";
            currentStock.isPositive = false;
        }

        displayStock(currentStock);
    }

    private void addStockToWatchlistView(StockItem stock) {
        if (llWatchlistContainer == null) return;

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#2A2E39"));
        llWatchlistContainer.addView(divider);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 12, 0, 12);

        TextView tvName = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvName.setLayoutParams(nameParams);
        tvName.setText(stock.symbol + " (" + stock.companyName + ")");
        tvName.setTextColor(Color.parseColor("#FFFFFF"));
        tvName.setTextSize(13);
        tvName.setTypeface(null, Typeface.BOLD);
        row.addView(tvName);

        TextView tvPrice = new TextView(this);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        priceParams.setMargins(0, 0, 20, 0);
        tvPrice.setLayoutParams(priceParams);
        tvPrice.setText(stock.price);
        tvPrice.setTextColor(Color.parseColor("#FFFFFF"));
        tvPrice.setTextSize(13);
        row.addView(tvPrice);

        TextView tvChange = new TextView(this);
        tvChange.setText(stock.change);
        tvChange.setTextSize(12);
        tvChange.setTypeface(null, Typeface.BOLD);
        tvChange.setTextColor(stock.isPositive ? Color.parseColor("#26A69A") : Color.parseColor("#EF5350"));
        row.addView(tvChange);

        row.setOnClickListener(v -> displayStock(stock));

        llWatchlistContainer.addView(row);
    }
}
