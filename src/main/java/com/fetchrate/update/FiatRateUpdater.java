package com.fetchrate.update;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;
import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.config.ECBURLs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * This class serves to combine the fetcher and the parser in order to return an ArrayList of ExchangeRateRecords
 */
@Service
public class FiatRateUpdater {

    private String URLtoBeUsed;
    private final FiatRateParser fiatRateParser;
    private final FiatRateFetcher fiatRateFetcher;
    private final ECBURLs URL;
    private final RateDatabase database;

    public FiatRateUpdater(FiatRateParser fiatRateParser, FiatRateFetcher fiatRateFetcher, ECBURLs URL, RateDatabase database) {
        this.fiatRateParser = fiatRateParser;
        this.fiatRateFetcher = fiatRateFetcher;
        this.URL = URL;
        this.database = database;
    }

    /**
     * This method chooses the appropriate URL depending on the latest update in the database.
     */
    private void chooseECBURL() {

        LocalDate latestDate = database.findLatestFiatDate();

        long daysBehind = ChronoUnit.DAYS.between(latestDate, LocalDate.now());

        if (latestDate == null) {
            URLtoBeUsed = URL.getFullURL();
        }

        if (daysBehind >= 90) {
            URLtoBeUsed = URL.getFullURL();

        }

        if (daysBehind < 90 && daysBehind > 1) {
            URLtoBeUsed = URL.getDays90URL();

        }

        if (daysBehind == 1) {
            URLtoBeUsed = URL.getDailyURL();

        }

    }

    /**
     * This method combines the fetching and the parsing and returns a finalized ArrayList of the Records.
     *
     * @return ArrayList of ExchangeRateRecord type.
     */
    public List<ExchangeRateRecord> fetchAndParseFiat() {
        chooseECBURL();
        String xml = fiatRateFetcher.fetchFiat(URLtoBeUsed);
        return fiatRateParser.parseFiat(xml);
    }
}
