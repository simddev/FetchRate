package com.fetchrate.update;

import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * This class serves to combine both Fiat and Crypto updates into one process and consequentially store their end results,
 * their data in a database via the RateDatabase classes from the persistence package.
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
     * This method is the end point of the Fiat and Crypto update branches and stores their result in a database,
     * located at /FetchRate/data via the RateDatabase methods.
     */
    public void updateRates() {
        //cryptoUpdate.update();
        database.initSchema();

        List<ExchangeRateRecord> fiatRecord = fiatUpdate.fetchAndParseFiat();

        database.updateFiatRates(fiatRecord);
    }
}
