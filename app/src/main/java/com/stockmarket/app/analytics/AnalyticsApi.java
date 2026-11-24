package com.stockmarket.app.analytics;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.stockmarket.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for the Analytics & Reports endpoints of the backend (see docs/api-documentation.md).
 * All calls run on a background thread and deliver results on the main thread.
 * When the backend cannot be reached, chart/compare calls fall back to {@link OfflineAnalytics}
 * so the screens still work in a demo; report downloads report the error instead.
 */
public class AnalyticsApi {

    public interface Callback<T> {
        void onResult(T result);

        void onError(String message);
    }

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private final String baseUrl;
    private final Handler main = new Handler(Looper.getMainLooper());

    public AnalyticsApi(Context context) {
        String url = context.getString(R.string.api_base_url);
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String baseUrl() {
        return baseUrl;
    }

    /** Comparison of 1..4 symbols over the last {@code bars} daily bars. Never fails: falls back to offline data. */
    public void compare(List<String> symbols, int bars, Callback<Comparison> callback) {
        EXECUTOR.execute(() -> {
            Comparison result;
            try {
                String json = new String(get(url("/api/analytics/compare", symbols, bars)), StandardCharsets.UTF_8);
                result = parseComparison(new JSONObject(json));
            } catch (Exception e) {
                result = OfflineAnalytics.compare(symbols, bars);
            }
            Comparison r = result;
            main.post(() -> callback.onResult(r));
        });
    }

    /**
     * Downloads a report ("text", "pdf" or "excel") into the app cache.
     * File name comes from the server's Content-Disposition header.
     */
    public void downloadReport(Context context, String format, List<String> symbols, int bars, Callback<File> callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                HttpURLConnection conn = open(url("/api/reports/" + format, symbols, bars));
                try {
                    int code = conn.getResponseCode();
                    if (code != 200) throw new IOException(serverError(conn, code));
                    File dir = new File(app.getCacheDir(), "reports");
                    if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create cache folder");
                    File out = new File(dir, fileName(conn, format, symbols));
                    try (InputStream in = conn.getInputStream(); OutputStream os = new FileOutputStream(out)) {
                        copy(in, os);
                    }
                    main.post(() -> callback.onResult(out));
                } finally {
                    conn.disconnect();
                }
            } catch (IOException e) {
                String msg = e.getMessage() != null && e.getMessage().startsWith("Server: ")
                        ? e.getMessage().substring(8)
                        : "Report server not reachable at " + baseUrl + ". Start the backend and try again.";
                main.post(() -> callback.onError(msg));
            }
        });
    }

    // ---------------------------------------------------------------- HTTP

    private String url(String path, List<String> symbols, int bars) {
        Uri.Builder b = Uri.parse(baseUrl + path).buildUpon()
                .appendQueryParameter("symbols", join(symbols))
                .appendQueryParameter("interval", "1day")
                .appendQueryParameter("outputsize", Integer.toString(bars));
        return b.build().toString();
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        return conn;
    }

    private byte[] get(String url) throws IOException {
        HttpURLConnection conn = open(url);
        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new IOException(serverError(conn, code));
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                copy(in, out);
                return out.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Server errors are JSON: { "error": "message" }. Prefixed with "Server: " so callers can show them as-is. */
    private static String serverError(HttpURLConnection conn, int code) {
        try (InputStream err = conn.getErrorStream()) {
            if (err != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                copy(err, out);
                String msg = new JSONObject(out.toString("UTF-8")).optString("error", "");
                if (!msg.isEmpty()) return "Server: " + msg;
            }
        } catch (IOException | JSONException ignored) {
            // fall through to the generic message
        }
        return "Server: Request failed (HTTP " + code + ")";
    }

    private static String fileName(HttpURLConnection conn, String format, List<String> symbols) {
        String cd = conn.getHeaderField("Content-Disposition");
        if (cd != null) {
            int i = cd.indexOf("filename=\"");
            if (i >= 0) {
                int end = cd.indexOf('"', i + 10);
                String name = end > i ? cd.substring(i + 10, end) : "";
                // Only keep safe characters: the name becomes a local file path.
                if (name.matches("[A-Za-z0-9._-]{1,100}")) return name;
            }
        }
        String ext = "pdf".equals(format) ? "pdf" : "excel".equals(format) ? "xlsx" : "txt";
        return "stock-report_" + join(symbols).replace(',', '_') + "." + ext;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static String join(List<String> symbols) {
        StringBuilder sb = new StringBuilder();
        for (String s : symbols) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- JSON

    static Comparison parseComparison(JSONObject json) throws JSONException {
        Comparison c = new Comparison();
        c.dataSource = json.optString("dataSource", "DEMO");
        c.bestPerformer = json.optString("bestPerformer", null);
        c.worstPerformer = json.optString("worstPerformer", null);
        c.leastVolatile = json.optString("leastVolatile", null);
        c.stocks = new ArrayList<>();
        JSONArray stocks = json.getJSONArray("stocks");
        for (int i = 0; i < stocks.length(); i++) c.stocks.add(parseSeries(stocks.getJSONObject(i)));
        return c;
    }

    static StockSeries parseSeries(JSONObject json) throws JSONException {
        JSONObject q = json.getJSONObject("quote");
        JSONObject st = json.getJSONObject("stats");
        JSONArray h = json.getJSONArray("history");
        JSONArray ma = json.getJSONArray("movingAverage");

        StockSeries s = new StockSeries();
        s.symbol = q.getString("symbol");
        s.name = q.optString("name", s.symbol);
        s.exchange = q.optString("exchange", "");
        s.currency = q.optString("currency", "");
        s.price = q.optDouble("price", 0);
        s.percentChange = q.optDouble("percentChange", 0);

        int n = h.length();
        s.dates = new ArrayList<>(n);
        s.closes = new double[n];
        s.movingAverage = new double[n];
        for (int i = 0; i < n; i++) {
            JSONObject p = h.getJSONObject(i);
            s.dates.add(p.getString("datetime"));
            s.closes[i] = p.getDouble("close");
            s.movingAverage[i] = ma.isNull(i) ? Double.NaN : ma.getDouble(i);
        }

        s.startPrice = st.optDouble("startPrice", 0);
        s.returnPercent = st.optDouble("returnPercent", 0);
        s.high = st.optDouble("high", 0);
        s.low = st.optDouble("low", 0);
        s.averageClose = st.optDouble("averageClose", 0);
        s.volatilityPercent = st.optDouble("dailyVolatilityPercent", 0);
        s.averageVolume = st.optLong("averageVolume", 0);
        s.movingAverageWindow = st.optInt("movingAverageWindow", 0);
        s.movingAverageValue = st.optDouble("movingAverage", 0);
        s.trend = st.optString("trend", "");
        return s;
    }
}
