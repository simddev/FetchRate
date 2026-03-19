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
 * This class serves to combine both Fiat and Crypto updates into one process and consequentially store their end results,
 * their data in a database via the RateDatabase classes from the persistence package.
 * <p>
 * It only runs if the last update is older than a day. Once it updates, it overwrites the latest update date.
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
     * This method is the end point of the Fiat and Crypto update branches and stores their result in a database,
     * located at /FetchRate/data via the RateDatabase methods.
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
