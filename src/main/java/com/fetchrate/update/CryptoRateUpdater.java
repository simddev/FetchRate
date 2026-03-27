package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.persistence.RateDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Coordinates fetching and parsing of cryptocurrency exchange rates from all available sources.
 * On a full update, loads rates from local CSV files and, if an API key is configured,
 * also fetches the last 30 days from the configured crypto data provider API. Supports lazy single-date
 * fetching for on-demand lookups when a rate is missing from the database.
 */
@Service
public class CryptoRateUpdater {

    private static final Logger log = LoggerFactory.getLogger(CryptoRateUpdater.class);

    /** The default set of symbols fetched on each daily update when no custom list is configured. */
    public static final List<String> DEFAULT_SYMBOLS = List.of("BTC", "ETH", "LTC", "DOGE", "SOL", "USDT");

    private final CryptoRateFetcher fetcher;
    private final CryptoRateParser parser;
    private final RateDatabase database;

    public CryptoRateUpdater(CryptoRateFetcher fetcher, CryptoRateParser parser, RateDatabase database) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.database = database;
    }

    /**
     * Returns the effective list of tracked symbols: the custom list from the database if one has been
     * configured, or {@link #DEFAULT_SYMBOLS} if the database table is empty.
     */
    public List<String> getEffectiveSymbols() {
        List<String> db = database.getTrackedSymbols();
        return db.isEmpty() ? DEFAULT_SYMBOLS : db;
    }

    /**
     * Returns {@code true} if the user has configured a custom tracked-symbol list.
     */
    public boolean isCustomized() {
        return !database.getTrackedSymbols().isEmpty();
    }

    /**
     * Adds a symbol to the tracked list.
     * If no custom list exists yet, the default list is seeded first so that
     * the existing symbols are preserved alongside the new one.
     *
     * @param symbol The coin symbol to add (e.g., {@code "XRP"}).
     */
    public void addTrackedSymbol(String symbol) {
        if (database.getTrackedSymbols().isEmpty()) {
            for (String def : DEFAULT_SYMBOLS) {
                database.addTrackedSymbol(def);
            }
        }
        database.addTrackedSymbol(symbol);
    }

    /**
     * Removes a symbol from the tracked list.
     * If no custom list exists yet, the default list is seeded first so that
     * only the specified symbol is removed while all others are retained.
     *
     * @param symbol The coin symbol to remove.
     */
    public void removeTrackedSymbol(String symbol) {
        if (database.getTrackedSymbols().isEmpty()) {
            for (String def : DEFAULT_SYMBOLS) {
                database.addTrackedSymbol(def);
            }
        }
        database.removeTrackedSymbol(symbol);
    }

    /**
     * Fetches and parses rates for a specific coin around a single date via the crypto data provider API.
     * Requests a 3-day window centred on the target date to handle timezone edge cases.
     * Returns an empty list if no API key is available or if the fetch fails.
     *
     * @param symbol The coin symbol (e.g., {@code BTC}).
     * @param date   The date for which a rate is needed.
     * @return Parsed records for the requested window, or an empty list on failure.
     */
    public List<CryptoRateRecord> fetchAndParseSpecific(String symbol, LocalDate date) {
        if (!fetcher.isApiKeyAvailable()) {
            return List.of();
        }

        try {
            // Fetch a 3-day window to be safe and potentially provide adjacent dates
            LocalDate start = date.minusDays(1);
            LocalDate end = date.plusDays(1);
            String json = fetcher.fetchFromLiveCoinWatch(symbol, start, end);
            return parser.parseLiveCoinWatch(symbol, json);
        } catch (Exception e) {
            log.error("Failed to lazy-fetch crypto data for {} on {}: {}", symbol, date, e.getMessage());
            return List.of();
        }
    }

    /**
     * Loads all available crypto rates from CSV files and, if an API key is configured,
     * fetches the last 30 days from the crypto data provider for a standard set of coins
     * (BTC, ETH, LTC, DOGE, SOL, USDT). API errors per symbol are logged and skipped.
     *
     * @return Combined list of {@link CryptoRateRecord} ready for database insertion.
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
        if (fetcher.isApiKeyAvailable()) {
            log.info("Using crypto data provider API for recent crypto rates...");
            List<String> symbolsToUpdate = getEffectiveSymbols();
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
                    log.error("Failed to fetch crypto data for {}: {}", symbol, e.getMessage());
                }
            }
        } else if (allRecords.isEmpty()) {
            log.warn("No API key and no CSV files found for crypto rates.");
        }

        return allRecords;
    }
}
