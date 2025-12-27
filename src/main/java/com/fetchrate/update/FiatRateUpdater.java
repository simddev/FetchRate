package com.fetchrate.update;

import org.springframework.stereotype.Service;
import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.config.ECBURLs;

import java.util.List;

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

    public List<ExchangeRateRecord> fetchAndParseFiat() {
        String xml = fiatRateFetcher.fetchFiat(URL.getDays90URL());
        return fiatRateParser.parseFiat(xml);
    }

}
