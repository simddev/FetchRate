package com.fetchrate.update;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves to acquire crypto exchange rate data, currently it is
 * acquired from .csv files which are located in /data/crypto.
 */
@Service
public class CryptoRateFetcher {

    private static final Path CRYPTO_DIR = Path.of("data", "crypto");

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
}
