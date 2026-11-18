# Real-Time Stock Market Dashboard

A Java-based stock market dashboard: a Spring Boot backend (REST API + MySQL) and an Android app.
Users register, log in, search stocks, see top gainers/losers, keep a personal watchlist, compare
stocks, and export reports.

## Team and modules

| Member | Module | Folder |
|---|---|---|
| 1 | Authentication and Database | `backend/stock-service/.../auth`, `.../watchlist`, `database/` |
| 2 | Stock API and Backend | `backend/stock-service/.../controller`, `.../service` |
| 3 | Android App / UI | `app/` |
| 4 | Analytics and Reports | `backend/report-service`, `app/.../CompareActivity`, `ReportGenerator` |

## Run the backend

Requirements: JDK 17+. MySQL 8 (or use the built-in H2 profile).

```bash
cd backend/stock-service

# 1. Set secrets as environment variables (never commit them)
#    PowerShell:  $env:TWELVE_DATA_API_KEY="your-key"; $env:DB_PASSWORD="your-mysql-password"
#    Mac/Linux:   export TWELVE_DATA_API_KEY=your-key DB_PASSWORD=your-mysql-password
#    Optional:    JWT_SECRET (32+ characters), DB_USERNAME (default root), DB_URL

# 2a. With MySQL (database "stockmarket" is created automatically)
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 2b. Without MySQL (local file database)
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

The server starts on `http://localhost:8080`. Full endpoint list: [`docs/api-documentation.md`](docs/api-documentation.md).
Database design: [`database/schema.sql`](database/schema.sql).

## Run the Android app
Open the project root in Android Studio and run the `app` module. The emulator reaches the backend at
`http://10.0.2.2:8080`.

## Security notes
- Passwords are stored as BCrypt hashes. Authentication uses JWT bearer tokens.
- API keys, database passwords and the JWT secret come from environment variables - never commit them.
