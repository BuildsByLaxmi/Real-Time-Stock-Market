package com.example.stockmarket.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    Optional<WatchlistItem> findByUserIdAndSymbol(Long userId, String symbol);
}
