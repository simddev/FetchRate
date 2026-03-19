package com.fetchrate.adapters.http;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides endpoints for reading and writing runtime settings,
 * such as the LiveCoinWatch API key and provider URL stored in the local database.
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
        String url = database.getMeta("livecoinwatch_history_url");
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyConfigured", key != null && !key.isBlank());
        result.put("providerUrl", (url != null && !url.isBlank()) ? url : null);
        return result;
    }

    @PostMapping
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        String providerUrl = body.get("providerUrl");

        boolean hasKey = apiKey != null && !apiKey.isBlank();
        boolean hasUrl = providerUrl != null && !providerUrl.isBlank();

        if (!hasKey && !hasUrl) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least one setting must be provided"));
        }

        if (hasKey) {
            database.setMeta("livecoinwatch_api_key", apiKey.trim());
        }
        if (hasUrl) {
            database.setMeta("livecoinwatch_history_url", providerUrl.trim());
        }

        return ResponseEntity.ok(Map.of("status", "saved"));
    }
}
