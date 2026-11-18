package com.example.stockmarket.watchlist;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist",
        uniqueConstraints = @UniqueConstraint(name = "uq_watchlist_user_symbol",
                columnNames = {"user_id", "symbol"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    protected WatchlistItem() {
    }

    public WatchlistItem(Long userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
    }

    @PrePersist
    void onCreate() {
        this.addedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
