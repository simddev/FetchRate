package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.persistence.RateDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Top-level orchestrator for refreshing the local rate database.
 * Runs fiat and crypto updates independently so that a failure in one source
 * does not prevent the other from completing. The {@code last_update} timestamp
 * is only written when at least one source succeeds, so a total network outage
 * will not suppress a retry for the rest of the day.
 */
@Service
public class RateUpdater {

    private static final Logger log = LoggerFactory.getLogger(RateUpdater.class);

    private final CryptoRateUpdater cryptoUpdate;
    private final FiatRateUpdater fiatUpdate;
    private final RateDatabase database;

    public RateUpdater(CryptoRateUpdater cryptoUpdate, FiatRateUpdater fiatUpdate, RateDatabase database) {
        this.cryptoUpdate = cryptoUpdate;
        this.fiatUpdate = fiatUpdate;
        this.database = database;
    }

    /**
     * Checks the small meta table in the database to see if the database is up to date.
     * @return True if updated, False if not
     */
    public boolean alreadyUpdatedToday() {
        LocalDate last = database.getLastUpdate();
        return last != null && last.equals(LocalDate.now());
    }


    /**
     * Fetches and persists the latest fiat and crypto exchange rates.
     * If the database was already updated today this method returns immediately.
     * Synchronized to prevent concurrent update runs when the HTTP server handles
     * multiple requests simultaneously.
     */
    public synchronized void updateRates() {

        if (alreadyUpdatedToday()) {
            return;
        }

        log.info("Updating database, please wait...");

        boolean fiatOk = false;
        boolean cryptoOk = false;

        try {
            List<FiatRateRecord> fiatRecord = fiatUpdate.fetchAndParseFiat();
            database.updateFiatRates(fiatRecord);
            fiatOk = true;
        } catch (Exception e) {
            log.error("Failed to update fiat rates: {}", e.getMessage());
        }

        try {
            List<CryptoRateRecord> cryptoRecord = cryptoUpdate.fetchAndParseCrypto();
            if (cryptoRecord != null && !cryptoRecord.isEmpty()) {
                database.updateCryptoRates(cryptoRecord);
            }
            cryptoOk = true;
        } catch (Exception e) {
            log.error("Failed to update crypto rates: {}", e.getMessage());
        }

        // Only mark as updated today if at least one data source succeeded,
        // so a full network failure does not suppress a retry for the rest of the day.
        if (fiatOk || cryptoOk) {
            database.setMeta("last_update", LocalDate.now().toString());
        }
    }

}
