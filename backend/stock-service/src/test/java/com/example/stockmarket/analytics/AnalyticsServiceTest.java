package com.example.stockmarket.analytics;

import com.example.stockmarket.analytics.data.DemoMarketDataSource;
import com.example.stockmarket.analytics.data.MarketData;
import com.example.stockmarket.analytics.data.MarketDataSource;
import com.example.stockmarket.analytics.model.ComparisonReport;
import com.example.stockmarket.analytics.model.PricePoint;
import com.example.stockmarket.analytics.model.StockQuote;
import com.example.stockmarket.analytics.model.StockReport;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AnalyticsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);

    /** Closes 100, 110, 99, 120, 120 -> known return, high/low, average. */
    private static MarketData fixed(String symbol, double... closes) {
        List<PricePoint> h = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            h.add(new PricePoint("2026-11-" + (10 + i), closes[i], closes[i] + 1, closes[i] - 1, closes[i], 1000L * (i + 1)));
        }
        StockQuote q = new StockQuote(symbol, symbol + " Inc", "NASDAQ", "USD", closes[closes.length - 1],
                0, 0, 0, 0, 0, 0, 0, null, null);
        return new MarketData(q, h);
    }

    @Test
    void computesStatsFromHistory() {
        StockReport r = AnalyticsService.buildReport(fixed("AAA", 100, 110, 99, 120, 120));

        assertThat(r.stats().startPrice()).isEqualTo(100);
        assertThat(r.stats().endPrice()).isEqualTo(120);
        assertThat(r.stats().returnPercent()).isEqualTo(20.0);
        assertThat(r.stats().high()).isEqualTo(121);
        assertThat(r.stats().low()).isEqualTo(98);
        assertThat(r.stats().averageClose()).isEqualTo(109.8);
        assertThat(r.stats().averageVolume()).isEqualTo(3000);
        assertThat(r.stats().dailyVolatilityPercent()).isGreaterThan(0);
        // window = max(2, min(20, 5/3)) = 2 -> last SMA = (120+120)/2
        assertThat(r.stats().movingAverageWindow()).isEqualTo(2);
        assertThat(r.stats().movingAverage()).isEqualTo(120);
        assertThat(r.stats().trend()).isEqualTo("UPTREND");
        assertThat(r.movingAverage()).hasSize(5);
        assertThat(r.movingAverage().get(0)).isNull();
        assertThat(r.movingAverage().get(1)).isEqualTo(105.0);
    }

    @Test
    void volatilityIsSampleStdDevOfReturns() {
        // returns: +10%, -10% -> mean 0, sample variance = (0.01+0.01)/1 -> stdev 0.1414 -> 14.14%
        StockReport r = AnalyticsService.buildReport(fixed("AAA", 100, 110, 99));
        assertThat(r.stats().dailyVolatilityPercent()).isCloseTo(14.14, within(0.01));
    }

    @Test
    void compareRanksBestWorstAndCalmest() {
        MarketDataSource source = new MarketDataSource() {
            public String name() { return "TEST"; }
            public MarketData fetch(String symbol, String interval, int size) {
                return switch (symbol) {
                    case "UP" -> fixed(symbol, 100, 101, 102, 103, 104);       // +4%, very smooth
                    case "DOWN" -> fixed(symbol, 100, 90, 95, 85, 80);         // -20%
                    default -> fixed(symbol, 100, 120, 100, 130, 110);         // +10%, choppy
                };
            }
        };
        ComparisonReport c = new AnalyticsService(source, CLOCK).compare("up, down ,wild", "1day", 5);

        assertThat(c.stocks()).extracting(StockReport::symbol).containsExactly("UP", "DOWN", "WILD");
        assertThat(c.bestPerformer()).isEqualTo("WILD");
        assertThat(c.worstPerformer()).isEqualTo("DOWN");
        assertThat(c.leastVolatile()).isEqualTo("UP");
        assertThat(c.generatedAt()).isEqualTo(CLOCK.instant());
        assertThat(c.dataSource()).isEqualTo("TEST");
    }

    @Test
    void cachesRepeatedRequestsForSameWindow() {
        AtomicInteger calls = new AtomicInteger();
        MarketDataSource source = new MarketDataSource() {
            public String name() { return "TEST"; }
            public MarketData fetch(String symbol, String interval, int size) {
                calls.incrementAndGet();
                return fixed(symbol, 1, 2, 3, 4, 5);
            }
        };
        AnalyticsService service = new AnalyticsService(source, CLOCK);
        service.compare("AAPL,MSFT", "1day", 30);
        service.compare("MSFT,AAPL", "1day", 30);
        service.history("aapl", "1day", 30);
        assertThat(calls).hasValue(2);

        service.history("AAPL", "1week", 30); // different window -> new fetch
        assertThat(calls).hasValue(3);
    }

    @Test
    void validatesInput() {
        AnalyticsService service = new AnalyticsService(new DemoMarketDataSource(), CLOCK);

        assertThatThrownBy(() -> service.compare("", "1day", 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.compare("A,B,C,D,E", "1day", 30))
                .hasMessageContaining("at most 4");
        assertThatThrownBy(() -> service.compare("AAPL&x=1", "1day", 30))
                .hasMessageContaining("Invalid symbol");
        assertThatThrownBy(() -> service.compare("AAPL", "2day", 30))
                .hasMessageContaining("Invalid interval");
        assertThatThrownBy(() -> service.compare("AAPL", "1day", 4))
                .hasMessageContaining("outputsize");
        assertThatThrownBy(() -> service.compare("AAPL", "1day", 501))
                .hasMessageContaining("outputsize");
        // duplicates collapse, so 5 entries with one repeat is fine
        assertThat(service.compare("a,b,c,d,a", "1day", 10).stocks()).hasSize(4);
    }

    @Test
    void demoDataIsDeterministicAndShapedCorrectly() {
        DemoMarketDataSource demo = new DemoMarketDataSource();
        MarketData a = demo.fetch("AAPL", "1day", 60);
        MarketData b = demo.fetch("AAPL", "1day", 60);

        assertThat(a.history()).hasSize(60).isEqualTo(b.history());
        assertThat(a.quote().price()).isEqualTo(225.80);
        assertThat(a.quote().name()).isEqualTo("Apple Inc.");
        assertThat(a.history()).allSatisfy(p -> {
            assertThat(p.high()).isGreaterThanOrEqualTo(Math.max(p.open(), p.close()));
            assertThat(p.low()).isLessThanOrEqualTo(Math.min(p.open(), p.close()));
        });
        // oldest first, strictly increasing dates
        for (int i = 1; i < a.history().size(); i++) {
            assertThat(a.history().get(i).datetime()).isGreaterThan(a.history().get(i - 1).datetime());
        }
        assertThat(demo.fetch("AAPL", "15min", 20).history().get(0).datetime()).hasSize(19);
    }
}
