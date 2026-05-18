package com.fetchrate.adapters.http;

import com.fetchrate.persistence.RateDatabase;
import com.fetchrate.update.CryptoRateUpdater;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides endpoints for reading and writing runtime settings,
 * such as the crypto data provider API key and URL stored in the local database.
 */
@Profile("http")
@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final RateDatabase database;
    private final CryptoRateUpdater cryptoUpdater;

    public SettingsController(RateDatabase database, CryptoRateUpdater cryptoUpdater) {
        this.database = database;
        this.cryptoUpdater = cryptoUpdater;
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
        String key = database.getMeta("crypto_api_key");
        String url = database.getMeta("crypto_provider_url");
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyConfigured", key != null && !key.isBlank());
        result.put("providerUrl", (url != null && !url.isBlank()) ? url : null);
        result.put("trackedSymbols", cryptoUpdater.getEffectiveSymbols());
        result.put("trackedSymbolsCustomized", cryptoUpdater.isCustomized());
        return result;
    }

    /**
     * Saves runtime settings to the database.
     * At least one of the following fields must be present and non-blank:
     * {@code apiKey}, {@code providerUrl}, {@code addSymbol}, or {@code removeSymbol}.
     * The provider URL, if supplied, must use the {@code http} or {@code https} scheme.
     * Symbols must be 2–10 uppercase alphanumeric characters.
     * All validation runs before any write is performed.
     *
     * @param body Request body with optional fields: {@code apiKey}, {@code providerUrl},
     *             {@code addSymbol}, {@code removeSymbol}.
     * @return 200 OK on success, 400 Bad Request if no valid field is provided or any value is invalid.
     */
    @PostMapping
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        String providerUrl = body.get("providerUrl");
        String addSymbol = body.get("addSymbol");
        String removeSymbol = body.get("removeSymbol");

        boolean hasKey = apiKey != null && !apiKey.isBlank();
        boolean hasUrl = providerUrl != null && !providerUrl.isBlank();
        boolean hasAddSymbol = addSymbol != null && !addSymbol.isBlank();
        boolean hasRemoveSymbol = removeSymbol != null && !removeSymbol.isBlank();

        if (!hasKey && !hasUrl && !hasAddSymbol && !hasRemoveSymbol) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least one setting must be provided"));
        }

        // Validate all fields before writing anything
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
        }
        if (hasAddSymbol) {
            String sym = addSymbol.trim().toUpperCase();
            if (!sym.matches("^[A-Z0-9]{2,10}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Symbol must be 2–10 alphanumeric characters"));
            }
        }
        if (hasRemoveSymbol) {
            String sym = removeSymbol.trim().toUpperCase();
            if (!sym.matches("^[A-Z0-9]{2,10}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Symbol must be 2–10 alphanumeric characters"));
            }
        }

        // Execute only after all validations pass
        if (hasKey) {
            database.setMeta("crypto_api_key", apiKey.trim());
        }
        if (hasUrl) {
            database.setMeta("crypto_provider_url", providerUrl.trim());
        }
        if (hasAddSymbol) {
            cryptoUpdater.addTrackedSymbol(addSymbol.trim().toUpperCase());
        }
        if (hasRemoveSymbol) {
            cryptoUpdater.removeTrackedSymbol(removeSymbol.trim().toUpperCase());
        }

        return ResponseEntity.ok(Map.of("status", "saved"));
    }
}
