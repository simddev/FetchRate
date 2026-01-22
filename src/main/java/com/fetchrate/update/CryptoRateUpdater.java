package com.fetchrate.update;

import com.fetchrate.config.LiveCoinWatchConfig;
import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.CurrencyClassifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class serves to combine the fetcher and parser class and produce the final result.
 */
@Service
public class CryptoRateUpdater {

    private final CryptoRateFetcher fetcher;
    private final CryptoRateParser parser;
    private final LiveCoinWatchConfig config;
    private final CurrencyClassifier classifier;

    public CryptoRateUpdater(CryptoRateFetcher fetcher, CryptoRateParser parser, LiveCoinWatchConfig config, CurrencyClassifier classifier) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.config = config;
        this.classifier = classifier;
    }

    /**
     * Fetches and parses crypto rates for a specific symbol and date range.
     * Useful for lazy-loading historical data.
     */
    public List<CryptoRateRecord> fetchAndParseSpecific(String symbol, LocalDate date) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return List.of();
        }

        try {
            // Fetch a 3-day window to be safe and potentially provide adjacent dates
            LocalDate start = date.minusDays(1);
            LocalDate end = date.plusDays(1);
            String json = fetcher.fetchFromLiveCoinWatch(symbol, start, end);
            return parser.parseLiveCoinWatch(symbol, json);
        } catch (Exception e) {
            System.err.println("Failed to lazy-fetch LiveCoinWatch data for " + symbol + " on " + date + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * This method creates the final List by providing the raw data from the fetcher
     * to the parser.
     * @return List of CryptoRateRecord ready for database entry.
     */
    public List<CryptoRateRecord> fetchAndParseCrypto() {

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            System.out.println("Using LiveCoinWatch API for crypto rates...");
            List<CryptoRateRecord> allRecords = new ArrayList<>();
            // For now we fetch for all supported cryptos
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30); // Reduced to 30 days to avoid overloading and daily limits

            for (String symbol : classifier.getCurrencyNames().keySet()) {
                if (classifier.isCrypto(symbol)) {
                    try {
                        String json = fetcher.fetchFromLiveCoinWatch(symbol, start, end);
                        List<CryptoRateRecord> records = parser.parseLiveCoinWatch(symbol, json);
                        if (records.isEmpty()) {
                            System.out.println("No records returned from API for " + symbol);
                        } else {
                            allRecords.addAll(records);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to fetch LiveCoinWatch data for " + symbol + ": " + e.getMessage());
                    }
                }
            }
            // If we have an API key, we ignore CSVs as per user request
            return allRecords;
        }

        System.out.println("No API key found. Falling back to local CSV files...");
        Map<String, String> csvBySymbol = fetcher.fetchAllCsv();

        List<CryptoRateRecord> cryptoRateRecords = new ArrayList<>();
        for (var entry : csvBySymbol.entrySet()) {
            String symbol = entry.getKey();
            String csv = entry.getValue();
            cryptoRateRecords.addAll(parser.parseCrypto(symbol, csv));
        }

        return cryptoRateRecords;
    }
}
