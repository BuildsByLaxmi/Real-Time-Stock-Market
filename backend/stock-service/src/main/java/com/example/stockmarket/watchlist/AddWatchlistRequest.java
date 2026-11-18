package com.example.stockmarket.watchlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddWatchlistRequest {

    @NotBlank(message = "Symbol cannot be empty")
    @Size(max = 20, message = "Symbol must be at most 20 characters")
    private String symbol;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
}
