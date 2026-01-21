package com.fetchrate.update;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.config.ECBURLs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * This class serves to combine the fetcher and the parser in order to return a List of FiatRateRecords.
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
     * This method chooses the appropriate URL depending on the latest update in the database.
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
     * This method combines the fetching and the parsing and returns a finalized List of the Records.
     *
     * @return List of FiatRateRecord type.
     */
    public List<FiatRateRecord> fetchAndParseFiat() {
        String urlToBeUsed = chooseECBURL();
        String xml = fiatRateFetcher.fetchFiat(urlToBeUsed);
        return fiatRateParser.parseFiat(xml);
    }
}
