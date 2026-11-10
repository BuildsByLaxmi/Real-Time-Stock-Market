
package com.example.stockmarket.controller;

import com.example.stockmarket.service.StockService;
import com.example.stockmarket.dto.StockSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("/search-stock")
    public String searchStock(@RequestParam String symbol) {
        return stockService.getStockDetails(symbol);
    }

    @GetMapping("/stock-details")
    public String stockDetails(@RequestParam String symbol) {
        return stockService.getStockDetails(symbol);
    }

    @GetMapping("/top-gainers")
    public List<StockSummary> topGainers() {
        return stockService.getTopGainers();
    }

    @GetMapping("/top-losers")
    public List<StockSummary> topLosers() {
        return stockService.getTopLosers();
    }
}
