package com.fetchrate.update;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.config.ECBURLs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Coordinates fetching and parsing of ECB fiat exchange rate data.
 * Selects the appropriate ECB feed URL based on how long ago the database was last updated:
 * daily, 90-day, or full historical.
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
     * Selects the ECB feed URL based on how far behind the database is.
     * Returns the full historical URL if the database has never been updated or is 90+ days behind,
     * the 90-day URL if 2–89 days behind, or the daily URL if only 1 day behind.
     */
    private String chooseECBURL() {

        LocalDate latestDate = database.getLastUpdate();

        if (latestDate == null) {
            return URL.getFullURL();
        } else {
            long daysBehind = ChronoUnit.DAYS.between(latestDate, LocalDate.now());
            if (daysBehind >= 90) return URL.getFullURL();
            else if (daysBehind > 1) return URL.getDays90URL();
            else return URL.getDailyURL();
        }

    }

    /**
     * Fetches the ECB XML feed for the appropriate date range and parses it into records.
     *
     * @return List of {@link com.fetchrate.core.FiatRateRecord} ready to be stored in the database.
     */
    public List<FiatRateRecord> fetchAndParseFiat() {
        String urlToBeUsed = chooseECBURL();
        String xml = fiatRateFetcher.fetchFiat(urlToBeUsed);
        return fiatRateParser.parseFiat(xml);
    }
}
