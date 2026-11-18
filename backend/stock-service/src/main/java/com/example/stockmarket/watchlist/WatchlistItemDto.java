package com.example.stockmarket.watchlist;

import java.time.LocalDateTime;

public class WatchlistItemDto {

    private final String symbol;
    private final LocalDateTime addedAt;

    public WatchlistItemDto(WatchlistItem item) {
        this.symbol = item.getSymbol();
        this.addedAt = item.getAddedAt();
    }

    public String getSymbol() { return symbol; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
