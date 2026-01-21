package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.persistence.RateDatabase;
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
        database.initSchema(); // safe to call, creates tables if missing
        LocalDate last = database.getLastUpdate();
        return last != null && last.equals(LocalDate.now());
    }


    /**
     * This method is the end point of the Fiat and Crypto update branches and stores their result in a database,
     * located at /FetchRate/data via the RateDatabase methods.
     */
    public void updateRates() {

        database.initSchema();

        List<FiatRateRecord> fiatRecord = fiatUpdate.fetchAndParseFiat();
        database.updateFiatRates(fiatRecord);

        List<CryptoRateRecord> cryptoRecord = cryptoUpdate.fetchAndParseCrypto();
        if (cryptoRecord.isEmpty()) {
            System.err.println("Crypto Database not created because of missing .csv files. Please put the appropriate .csv files in /data/crypto in order to update the Crypto Exchange Rate Database");
        } else {
            database.updateCryptoRates(cryptoRecord);
        }

        // Updates last update date after both tables are updated.
        database.setMeta("last_update", LocalDate.now().toString());
    }

}
