package com.example.stockmarket.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through Spring MVC + security, using demo data (analytics.demo-data=true in test properties). */
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void historyReturnsSeriesAndStats() throws Exception {
        mvc.perform(get("/api/analytics/history").param("symbol", "aapl").param("outputsize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quote.symbol").value("AAPL"))
                .andExpect(jsonPath("$.history.length()").value(20))
                .andExpect(jsonPath("$.movingAverage.length()").value(20))
                .andExpect(jsonPath("$.stats.trend").exists());
    }

    @Test
    void compareReturnsAllStocks() throws Exception {
        mvc.perform(get("/api/analytics/compare").param("symbols", "AAPL,MSFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("DEMO"))
                .andExpect(jsonPath("$.stocks.length()").value(2))
                .andExpect(jsonPath("$.stocks[1].quote.symbol").value("MSFT"))
                .andExpect(jsonPath("$.bestPerformer").exists());
    }

    @Test
    void chartIsPng() throws Exception {
        mvc.perform(get("/api/analytics/chart").param("symbols", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void reportsDownloadInEachFormat() throws Exception {
        mvc.perform(get("/api/reports/text").param("symbols", "AAPL,MSFT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=\"stock-comparison_AAPL_MSFT.txt\"")))
                .andExpect(content().string(startsWith("Stock Comparison: AAPL vs MSFT")));

        mvc.perform(get("/api/reports/pdf").param("symbols", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("stock-report_AAPL.pdf")));

        mvc.perform(get("/api/reports/excel").param("symbols", "AAPL").param("inline", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("inline; filename=\"stock-report_AAPL.xlsx\"")));
    }

    @Test
    void badInputReturnsJsonError() throws Exception {
        mvc.perform(get("/api/reports/word").param("symbols", "AAPL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Unknown report format")));

        mvc.perform(get("/api/analytics/compare").param("symbols", "A,B,C,D,E"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("at most 4")));

        mvc.perform(get("/api/analytics/chart").param("symbols", "AAPL").param("width", "99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mvc.perform(get("/api/analytics/history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing required parameter 'symbol'"));

        mvc.perform(get("/api/analytics/history").param("symbol", "AAPL").param("outputsize", "lots"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'outputsize'"));
    }
}
