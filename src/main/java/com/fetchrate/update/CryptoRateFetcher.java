package com.fetchrate.update;

import com.fetchrate.config.LiveCoinWatchConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves to acquire crypto exchange rate data.
 * It can fetch data from local .csv files in /data/crypto,
 * or from LiveCoinWatch API if an API key is provided.
 */
@Service
public class CryptoRateFetcher {

    private static final Path CRYPTO_DIR = Path.of("data", "crypto");
    private final LiveCoinWatchConfig config;
    private final HttpClient client = HttpClient.newHttpClient();

    public CryptoRateFetcher(LiveCoinWatchConfig config) {
        this.config = config;
    }

    /**
     * Reads all *.csv files in data/crypto and returns:
     *   symbol -> csv text
     */
    public Map<String, String> fetchAllCsv() {
        try {

            Files.createDirectories(CRYPTO_DIR);

            Map<String, String> collectionCsvData = new HashMap<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(CRYPTO_DIR, "*.csv")) {
                for (Path path : stream) {
                    String filename = path.getFileName().toString(); // Gets filename like BTC.csv
                    String symbol = filename.substring(0, filename.length() - 4).toUpperCase(); // Strips ".csv".
                    String csv = Files.readString(path);
                    collectionCsvData.put(symbol, csv);
                }
            }

            return collectionCsvData;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read crypto CSV files from " + CRYPTO_DIR, e);
        }
    }

    /**
     * Fetches historical rates for a given coin from LiveCoinWatch.
     *
     * @param symbol The crypto symbol (e.g., BTC).
     * @param start  Start date.
     * @param end    End date.
     * @return JSON response from API.
     */
    public String fetchFromLiveCoinWatch(String symbol, LocalDate start, LocalDate end) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("LiveCoinWatch API key is not configured.");
        }

        long startMs = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs = end.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        String body = String.format(
                "{\"currency\":\"EUR\",\"code\":\"%s\",\"start\":%d,\"end\":%d,\"meta\":false}",
                symbol, startMs, endMs
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getHistoryUrl()))
                .header("content-type", "application/json")
                .header("x-api-key", config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch from LiveCoinWatch: " + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error calling LiveCoinWatch API", e);
        }
    }
}
