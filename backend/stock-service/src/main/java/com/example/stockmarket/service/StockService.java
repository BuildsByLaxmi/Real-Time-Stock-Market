
package com.example.stockmarket.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.example.stockmarket.dto.StockSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class StockService {

    @Value("${stock.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Common list of stocks
    private static final List<String> STOCKS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN",
            "META", "TSLA", "NVDA", "NFLX"
    );

    // Get details of a particular stock
    public String getStockDetails(String symbol) {

        String url =
                "https://api.twelvedata.com/quote?symbol="
                        + symbol
                        + "&apikey="
                        + apiKey;

        return restTemplate.getForObject(url, String.class);
    }

    // Get Top 5 Gainers
    public List<StockSummary> getTopGainers() {

        List<StockSummary> result = new ArrayList<>();

        for (String symbol : STOCKS) {

            try {

                String url =
                        "https://api.twelvedata.com/quote?symbol="
                                + symbol
                                + "&apikey="
                                + apiKey;

                String response =
                        restTemplate.getForObject(url, String.class);

                JsonNode json = objectMapper.readTree(response);

                if (json.has("percent_change")
                        && !json.get("percent_change").isNull()) {

                    double change = Double.parseDouble(
                            json.get("percent_change").asString()
                    );

                    result.add(new StockSummary(symbol, change));
                }

            } catch (Exception e) {

                System.out.println(
                        "Failed to fetch " + symbol
                                + ": " + e.getMessage()
                );
            }
        }

        return result.stream()
                .sorted(
                        Comparator.comparing(
                                StockSummary::getChangePercent
                        ).reversed()
                )
                .limit(5)
                .toList();
    }

    // Get Top 5 Losers
    public List<StockSummary> getTopLosers() {

        List<StockSummary> result = new ArrayList<>();

        for (String symbol : STOCKS) {

            try {

                String url =
                        "https://api.twelvedata.com/quote?symbol="
                                + symbol
                                + "&apikey="
                                + apiKey;

                String response =
                        restTemplate.getForObject(url, String.class);

                JsonNode json = objectMapper.readTree(response);

                if (json.has("percent_change")
                        && !json.get("percent_change").isNull()) {

                    double change = Double.parseDouble(
                            json.get("percent_change").asString()
                    );

                    result.add(new StockSummary(symbol, change));
                }

            } catch (Exception e) {

                System.out.println(
                        "Failed to fetch " + symbol
                                + ": " + e.getMessage()
                );
            }
        }

        return result.stream()
                .sorted(
                        Comparator.comparing(
                                StockSummary::getChangePercent
                        )
                )
                .limit(5)
                .toList();
    }
}
