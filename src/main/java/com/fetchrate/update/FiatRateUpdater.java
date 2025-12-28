package com.fetchrate.update;

import org.springframework.stereotype.Service;
import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.config.ECBURLs;
import java.util.List;

/**
 * This class serves to combine the fetcher and the parser in order to return an ArrayList of ExchangeRateRecords
 */
@Service
public class FiatRateUpdater {

    private final FiatRateParser fiatRateParser;
    private final FiatRateFetcher fiatRateFetcher;
    private final ECBURLs URL;

    public FiatRateUpdater(FiatRateParser fiatRateParser, FiatRateFetcher fiatRateFetcher, ECBURLs URL) {
        this.fiatRateParser = fiatRateParser;
        this.fiatRateFetcher = fiatRateFetcher;
        this.URL = URL;
    }

    /**
     * This method combines the fetching and the parsing and returns a finalized ArrayList of the Records.
     * @return ArrayList of ExchangeRateRecord type.
     */
    public List<ExchangeRateRecord> fetchAndParseFiat() {
        String xml = fiatRateFetcher.fetchFiat(URL.getDays90URL()); // Change URL here through the getter
        return fiatRateParser.parseFiat(xml);
    }
}
