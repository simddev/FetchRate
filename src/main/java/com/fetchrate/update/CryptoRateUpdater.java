package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import org.springframework.stereotype.Service;

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

    public CryptoRateUpdater(CryptoRateFetcher fetcher, CryptoRateParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    /**
     * This method creates the final List by providing the raw data from the fetcher
     * to the parser.
     * @return List of CryptoRateRecord ready for database entry.
     */
    public List<CryptoRateRecord> fetchAndParseCrypto() {

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
