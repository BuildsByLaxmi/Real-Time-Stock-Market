package com.example.stockmarket.watchlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class WatchlistService {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9.\\-:]{1,20}$");

    private final WatchlistRepository repository;

    public WatchlistService(WatchlistRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemDto> list(Long userId) {
        return repository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(WatchlistItemDto::new)
                .toList();
    }

    @Transactional
    public WatchlistItemDto add(Long userId, String rawSymbol) {
        String symbol = normalize(rawSymbol);
        if (repository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new IllegalStateException(symbol + " is already in your watchlist");
        }
        return new WatchlistItemDto(repository.save(new WatchlistItem(userId, symbol)));
    }

    @Transactional
    public void remove(Long userId, String rawSymbol) {
        String symbol = normalize(rawSymbol);
        WatchlistItem item = repository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new NoSuchElementException(symbol + " is not in your watchlist"));
        repository.delete(item);
    }

    private String normalize(String raw) {
        String symbol = raw == null ? "" : raw.trim().toUpperCase();
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new IllegalArgumentException("Invalid stock symbol");
        }
        return symbol;
    }
}
