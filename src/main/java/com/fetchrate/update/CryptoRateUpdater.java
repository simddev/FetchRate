package com.fetchrate.update;

import com.fetchrate.config.LiveCoinWatchConfig;
import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.CurrencyClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CryptoRateUpdater.class);

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
            log.error("Failed to lazy-fetch LiveCoinWatch data for {} on {}: {}", symbol, date, e.getMessage());
            return List.of();
        }
    }

    /**
     * This method creates the final List by providing the raw data from the fetcher
     * to the parser.
     * @return List of CryptoRateRecord ready for database entry.
     */
    public List<CryptoRateRecord> fetchAndParseCrypto() {
        List<CryptoRateRecord> allRecords = new ArrayList<>();

        // 1) Always check for CSV files to fill gaps
        Map<String, String> csvBySymbol = fetcher.fetchAllCsv();
        for (var entry : csvBySymbol.entrySet()) {
            String symbol = entry.getKey();
            String csv = entry.getValue();
            allRecords.addAll(parser.parseCrypto(symbol, csv));
        }

        // 2) If API key is present, fetch the last 30 days via API
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            log.info("Using LiveCoinWatch API for recent crypto rates...");
            // We fetch for a fixed set of popular cryptos
            List<String> symbolsToUpdate = List.of("BTC", "ETH", "LTC", "DOGE", "SOL", "USDT");
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);

            for (String symbol : symbolsToUpdate) {
                try {
                    String json = fetcher.fetchFromLiveCoinWatch(symbol, start, end);
                    List<CryptoRateRecord> records = parser.parseLiveCoinWatch(symbol, json);
                    if (records.isEmpty()) {
                        log.warn("No records returned from API for {}", symbol);
                    } else {
                        allRecords.addAll(records);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch LiveCoinWatch data for {}: {}", symbol, e.getMessage());
                }
            }
        } else if (allRecords.isEmpty()) {
            log.warn("No API key and no CSV files found for crypto rates.");
        }

        return allRecords;
    }
}
