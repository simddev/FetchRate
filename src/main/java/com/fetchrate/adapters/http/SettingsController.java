package com.fetchrate.adapters.http;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

    /**
     * Returns the current runtime settings as a JSON object.
     * {@code apiKeyConfigured} is {@code true} when a non-blank API key is stored.
     * {@code providerUrl} contains the custom endpoint URL, or {@code null} if the default is used.
     *
     * @return Map containing {@code apiKeyConfigured} (Boolean) and {@code providerUrl} (String or null).
     */
    @GetMapping
    public Map<String, Object> getSettings() {
        String key = database.getMeta("livecoinwatch_api_key");
        String url = database.getMeta("livecoinwatch_history_url");
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyConfigured", key != null && !key.isBlank());
        result.put("providerUrl", (url != null && !url.isBlank()) ? url : null);
        return result;
    }

    /**
     * Saves one or both runtime settings to the database.
     * At least one field ({@code apiKey} or {@code providerUrl}) must be present and non-blank.
     * The provider URL, if supplied, must use the {@code http} or {@code https} scheme.
     *
     * @param body Request body containing optional {@code apiKey} and/or {@code providerUrl} fields.
     * @return 200 OK on success, 400 Bad Request if no valid field is provided or the URL is invalid.
     */
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
            try {
                URI uri = URI.create(providerUrl.trim());
                String scheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Provider URL must use http or https"));
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider URL format"));
            }
            database.setMeta("livecoinwatch_history_url", providerUrl.trim());
        }

        return ResponseEntity.ok(Map.of("status", "saved"));
    }
}
