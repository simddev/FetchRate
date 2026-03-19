package com.fetchrate.update;

import com.fetchrate.config.LiveCoinWatchConfig;
import com.fetchrate.persistence.RateDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Acquires cryptocurrency exchange rate data from two sources:
 * local CSV files placed in {@code data/crypto/}, and the LiveCoinWatch REST API
 * when an API key is available. The API key is resolved from the database first,
 * then falls back to the application properties file.
 */
@Service
public class CryptoRateFetcher {

    private final Path cryptoDir;
    private final LiveCoinWatchConfig config;
    private final RateDatabase database;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CryptoRateFetcher(@Value("${fetchrate.crypto-dir:data/crypto}") String cryptoDir,
                             LiveCoinWatchConfig config,
                             RateDatabase database) {
        this.cryptoDir = Path.of(cryptoDir);
        this.config = config;
        this.database = database;
    }

    /**
     * Resolves the LiveCoinWatch API key, preferring the value stored in the database
     * (set via the settings UI or CLI) over the application properties file.
     */
    private String resolveApiKey() {
        String dbKey = database.getMeta("livecoinwatch_api_key");
        if (dbKey != null && !dbKey.isBlank()) {
            return dbKey;
        }
        return config.getApiKey();
    }

    /**
     * Returns {@code true} if a non-blank LiveCoinWatch API key is available from either
     * the database or application properties.
     */
    public boolean isApiKeyAvailable() {
        String key = resolveApiKey();
        return key != null && !key.isBlank();
    }

    /**
     * Resolves the LiveCoinWatch history endpoint URL, preferring the value stored in the database
     * over the default from application properties.
     */
    private String resolveHistoryUrl() {
        String dbUrl = database.getMeta("livecoinwatch_history_url");
        if (dbUrl != null && !dbUrl.isBlank()) {
            return dbUrl;
        }
        return config.getHistoryUrl();
    }

    /**
     * Reads all {@code *.csv} files from the configured crypto directory (default: {@code data/crypto}).
     * Each file should be named after the coin symbol (e.g., {@code BTC.csv}).
     *
     * @return A map of upper-cased coin symbol to raw CSV content.
     * @throws RuntimeException if the directory cannot be read.
     */
    public Map<String, String> fetchAllCsv() {
        try {

            Files.createDirectories(cryptoDir);

            Map<String, String> collectionCsvData = new HashMap<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(cryptoDir, "*.csv")) {
                for (Path path : stream) {
                    String filename = path.getFileName().toString(); // Gets filename like BTC.csv
                    String symbol = filename.substring(0, filename.length() - 4).toUpperCase(); // Strips ".csv".
                    String csv = Files.readString(path);
                    collectionCsvData.put(symbol, csv);
                }
            }

            return collectionCsvData;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read crypto CSV files from " + cryptoDir, e);
        }
    }

    /**
     * Fetches historical EUR rates for a given coin from the LiveCoinWatch API.
     *
     * @param symbol The crypto symbol (e.g., {@code BTC}).
     * @param start  Start date (inclusive).
     * @param end    End date (inclusive).
     * @return Raw JSON response body from the API.
     * @throws IllegalStateException if no API key is configured.
     * @throws RuntimeException      if the API returns a non-200 status or the request fails.
     */
    public String fetchFromLiveCoinWatch(String symbol, LocalDate start, LocalDate end) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LiveCoinWatch API key is not configured.");
        }

        long startMs = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs = end.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        String body = String.format(
                "{\"currency\":\"EUR\",\"code\":\"%s\",\"start\":%d,\"end\":%d,\"meta\":false}",
                symbol, startMs, endMs
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resolveHistoryUrl()))
                .timeout(Duration.ofSeconds(15))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String responseBody = response.body();
                String snippet = responseBody != null && responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                throw new RuntimeException("Failed to fetch from LiveCoinWatch: " + response.statusCode() + " " + snippet);
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Error calling LiveCoinWatch API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error calling LiveCoinWatch API", e);
        }
    }
}
