package com.fetchrate.update;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;
import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.config.ECBURLs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * This class serves to combine the fetcher and the parser in order to return an ArrayList of ExchangeRateRecords
 */
@Service
public class FiatRateUpdater {

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
     * This method checks the latest entry in the database, in order to know what kind of update is needed,
     * it returns the URL for the daily exchange rate in any case, as this one can change within a day.
     * @return What kind of URL to use for the update.
     */
    private String chooseECBURL() {

        Optional<LocalDate> latestOpt = database.findLatestFiatDate();

        if (latestOpt.isEmpty()) {
            return URL.getFullURL();
        }

        LocalDate latest = latestOpt.get();
        long daysBehind = ChronoUnit.DAYS.between(latest, LocalDate.now());


        if (daysBehind > 90) {
            return URL.getFullURL();

        }

        if (daysBehind > 1) {
            return URL.getDays90URL();

        }

        return URL.getDailyURL();

    }

    /**
     * This method combines the fetching and the parsing and returns a finalized ArrayList of the Records.
     *
     * @return ArrayList of ExchangeRateRecord type.
     */
    public List<ExchangeRateRecord> fetchAndParseFiat() {
        String xml = fiatRateFetcher.fetchFiat(chooseECBURL());
        return fiatRateParser.parseFiat(xml);
    }
}
