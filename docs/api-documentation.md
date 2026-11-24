# API Documentation

Base URL (backend runs on port 8080):

| Client | Base URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080` |
| Real phone (same Wi-Fi) | `http://<your-PC-LAN-IP>:8080` |
| Browser / Postman on the PC | `http://localhost:8080` |

All bodies are JSON (`Content-Type: application/json`). Protected endpoints need the header
`Authorization: Bearer <token>` where the token comes from `/api/auth/login` or `/api/auth/register`.

**Error format (auth + watchlist endpoints):** `{ "error": "message" }`

| Status | Meaning |
|---|---|
| 400 | Validation problem (empty field, bad email, short password, bad symbol) |
| 401 | Wrong credentials, or missing/invalid/expired/logged-out token |
| 404 | Item not found (e.g. removing a symbol that is not in the watchlist) |
| 409 | Conflict (username/email already used, symbol already in watchlist) |

---

## Authentication (Member 1)

### POST `/api/auth/register`
Creates an account and logs the user in. **Public.**

Request
```json
{
  "fullName": "Demo Trader",
  "username": "demo",
  "email": "demo@stockmarket.com",
  "password": "123456"
}
```
Rules: `username` 3-50 chars (letters, digits, `_`, `.`), valid `email`, `password` 6-72 chars.
`fullName` is optional (defaults to the username).

Response `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "user": { "id": 1, "username": "demo", "email": "demo@stockmarket.com", "fullName": "Demo Trader" }
}
```
Errors: `400` validation, `409` `"Username is already taken"` / `"Email is already registered"`.

### POST `/api/auth/login`
**Public.** The `username` field accepts the username **or** the email address.

Request
```json
{ "username": "demo", "password": "123456" }
```
Response `200 OK`: same shape as register.
Errors: `401` `"Invalid username or password"`, `400` empty fields.

### POST `/api/auth/logout`
**Requires token.** Revokes the token on the server. The app should also delete its saved token.

Response `200 OK`
```json
{ "message": "Logged out successfully" }
```

---

## Watchlist (Member 1) - all require a token

### GET `/api/watchlist`
Returns the logged-in user's symbols, newest first.
```json
[
  { "symbol": "AAPL", "addedAt": "2026-11-11T10:15:30.123456" },
  { "symbol": "TSLA", "addedAt": "2026-11-10T18:02:11.000000" }
]
```

### POST `/api/watchlist`
Request `{ "symbol": "aapl" }` (stored upper-case). Response `201 Created`
```json
{ "symbol": "AAPL", "addedAt": "2026-11-11T10:15:30.123456" }
```
Errors: `409` already in watchlist, `400` invalid symbol.

### DELETE `/api/watchlist/{symbol}`
Example: `DELETE /api/watchlist/AAPL` -> `200 OK`
```json
{ "message": "Removed from watchlist" }
```
Errors: `404` symbol not in watchlist.

---

## Stock data (Member 2) - currently public

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/search-stock?symbol=AAPL` | Raw Twelve Data quote |
| GET | `/api/stock-details?symbol=AAPL` | Raw Twelve Data quote |
| GET | `/api/top-gainers` | `[{ "symbol": "...", "changePercent": 1.23 }]` |
| GET | `/api/top-losers` | same shape |

---

## Analytics & Reports (Member 4) - public

Uses live Twelve Data when `TWELVE_DATA_API_KEY` is set. Without a key (or with `ANALYTICS_DEMO_DATA=true`)
it serves **simulated demo data**, and every response and report is labelled `DEMO`. Each upstream result is
cached for 60 seconds, so downloading several formats in a row does not use up the free-plan quota
(8 requests/minute; every symbol costs 2 requests).

Common query parameters:

| Param | Default | Rules |
|---|---|---|
| `symbols` | - | 1-4 comma-separated tickers, e.g. `AAPL,MSFT` (duplicates ignored) |
| `interval` | `1day` | `1min`, `5min`, `15min`, `30min`, `1h`, `1day`, `1week`, `1month` |
| `outputsize` | `30` | number of bars, 5-500 |

| Method | Endpoint | Returns |
|---|---|---|
| GET | `/api/analytics/history?symbol=AAPL` | Price history + stats for one stock (JSON) |
| GET | `/api/analytics/compare?symbols=AAPL,MSFT` | Side-by-side comparison (JSON) |
| GET | `/api/analytics/chart?symbols=AAPL,MSFT` | PNG price chart. Optional `width` (300-2400, default 900) and `height` (200-1600, default 450) |
| GET | `/api/reports/text?symbols=AAPL,MSFT` | Plain-text report (`.txt`) |
| GET | `/api/reports/pdf?symbols=AAPL,MSFT` | PDF report: summary, metric table, charts, recent prices |
| GET | `/api/reports/excel?symbols=AAPL,MSFT` | Excel workbook (`.xlsx`): Summary, Performance (with a native Excel chart), one sheet per stock |

Report endpoints download as an attachment (`stock-comparison_AAPL_MSFT.pdf`). Add `inline=true` to open the
report in the browser instead. A single symbol gives a single-stock report (`stock-report_AAPL.pdf`).

One stock in the chart shows the close price plus a moving average. Several stocks show % change since the first
bar, so different price levels can be compared.

`GET /api/analytics/compare?symbols=AAPL,MSFT&outputsize=30` (shortened)
```json
{
  "generatedAt": "2026-11-24T10:58:48Z",
  "interval": "1day",
  "outputSize": 30,
  "dataSource": "TWELVE_DATA",
  "bestPerformer": "AAPL",
  "worstPerformer": "MSFT",
  "leastVolatile": "AAPL",
  "stocks": [{
    "quote": { "symbol": "AAPL", "name": "Apple Inc.", "exchange": "NASDAQ", "currency": "USD",
               "price": 225.8, "change": 2.68, "percentChange": 1.2, "open": 222.65, "high": 226.74,
               "low": 221.67, "previousClose": 223.12, "volume": 11980000,
               "fiftyTwoWeekHigh": 237.23, "fiftyTwoWeekLow": 164.08 },
    "stats": { "startPrice": 211.01, "endPrice": 225.8, "returnPercent": 7.01, "high": 231.57, "low": 200.8,
               "averageClose": 217.01, "dailyVolatilityPercent": 1.3, "averageVolume": 23760000,
               "movingAverageWindow": 10, "movingAverage": 225.2, "trend": "UPTREND" },
    "history": [ { "datetime": "2026-10-14", "open": 210.1, "high": 212.0, "low": 209.5, "close": 211.01, "volume": 18000000 } ],
    "movingAverage": [ null, null, 210.4 ]
  }]
}
```
- `history` is oldest first. `movingAverage` lines up with `history` and is `null` until the window is full.
- `returnPercent` is first close to last close. `dailyVolatilityPercent` is the standard deviation of bar-to-bar returns.
- `trend` is `UPTREND` when the last close is at or above the moving average.

Errors use `{ "error": "message" }`: `400` bad symbols/interval/size/format; `404` unknown symbol;
`429` market-data rate limit hit (wait a minute); `502` market-data provider unavailable.

---

## Android integration notes (for Member 3)

1. Add `implementation("com.squareup.retrofit2:retrofit:2.11.0")`, `retrofit2:converter-gson:2.11.0`
   (or OkHttp/Volley) to `app/build.gradle.kts`. `INTERNET` permission is already in the manifest.
2. Because the backend uses plain `http://`, add `android:usesCleartextTraffic="true"` to `<application>`
   in `AndroidManifest.xml` while developing.
3. Replace the local `UserRepositoryImpl` / SHA-256 `AuthService` with calls to `/api/auth/login` and
   `/api/auth/register`. Send **one** `username` field to login (username or email works).
4. Save `token` (for example in `SessionManager`) and send `Authorization: Bearer <token>` on watchlist calls.
5. On `401` from a protected call, clear the session and return to `LoginActivity`.
6. Replace the in-memory watchlist `HashSet` in `MainActivity` with `GET/POST/DELETE /api/watchlist`.
7. Network calls must run off the main thread (Retrofit `enqueue`, or an `ExecutorService`).

Minimal Retrofit interface:
```java
public interface AuthApi {
    @POST("api/auth/login")     Call<AuthResponse> login(@Body LoginRequest body);
    @POST("api/auth/register")  Call<AuthResponse> register(@Body RegisterRequest body);
    @POST("api/auth/logout")    Call<Void> logout(@Header("Authorization") String bearer);
    @GET("api/watchlist")       Call<List<WatchItem>> watchlist(@Header("Authorization") String bearer);
    @POST("api/watchlist")      Call<WatchItem> addToWatchlist(@Header("Authorization") String bearer, @Body AddRequest body);
    @DELETE("api/watchlist/{symbol}") Call<Void> removeFromWatchlist(@Header("Authorization") String bearer, @Path("symbol") String symbol);
}
```
