package com.example.stockmarket.watchlist;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    @GetMapping
    public List<WatchlistItemDto> list(Authentication auth) {
        return service.list(userId(auth));
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDto> add(Authentication auth,
                                                @Valid @RequestBody AddWatchlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(userId(auth), request.getSymbol()));
    }

    @DeleteMapping("/{symbol}")
    public Map<String, String> remove(Authentication auth, @PathVariable String symbol) {
        service.remove(userId(auth), symbol);
        return Map.of("message", "Removed from watchlist");
    }

    /** The JWT filter stores the user's id as the authentication principal. */
    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
