package com.fetchrate.adapters.http;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Provides endpoints for reading and writing runtime settings,
 * such as the LiveCoinWatch API key stored in the local database.
 */
@Profile("http")
@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final RateDatabase database;

    public SettingsController(RateDatabase database) {
        this.database = database;
    }

    @GetMapping
    public Map<String, Object> getSettings() {
        String key = database.getMeta("livecoinwatch_api_key");
        return Map.of("apiKeyConfigured", key != null && !key.isBlank());
    }

    @PostMapping
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "apiKey must not be empty"));
        }
        database.setMeta("livecoinwatch_api_key", apiKey.trim());
        return ResponseEntity.ok(Map.of("status", "saved"));
    }
}
