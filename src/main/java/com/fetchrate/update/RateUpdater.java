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
 * does not prevent the other from completing. Each source tracks its own
 * timestamp ({@code last_fiat_update} / {@code last_crypto_update}), written only
 * when that source succeeds, so a network outage will not suppress a retry for the rest of the day.
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
     * @return {@code true} if both fiat and crypto rates were already fetched today.
     */
    public boolean alreadyUpdatedToday() {
        LocalDate today = LocalDate.now();
        return today.equals(getMetaDate("last_fiat_update"))
                && today.equals(getMetaDate("last_crypto_update"));
    }

    private LocalDate getMetaDate(String key) {
        String v = database.getMeta(key);
        return (v == null) ? null : LocalDate.parse(v);
    }

    /**
     * Fetches and persists the latest fiat and crypto exchange rates.
     * Each source is tracked independently — a source that already succeeded today
     * is skipped, while a source that failed yesterday or earlier will retry.
     * Synchronized to prevent concurrent update runs when the HTTP server handles
     * multiple requests simultaneously.
     */
    public synchronized void updateRates() {
        LocalDate today = LocalDate.now();
        boolean fiatDone = today.equals(getMetaDate("last_fiat_update"));
        boolean cryptoDone = today.equals(getMetaDate("last_crypto_update"));

        if (fiatDone && cryptoDone) {
            return;
        }

        log.info("Updating database, please wait...");

        if (!fiatDone) {
            try {
                List<FiatRateRecord> fiatRecord = fiatUpdate.fetchAndParseFiat();
                database.updateFiatRates(fiatRecord);
                database.setMeta("last_fiat_update", today.toString());
            } catch (Exception e) {
                log.error("Failed to update fiat rates: {}", e.getMessage());
            }
        }

        if (!cryptoDone) {
            try {
                List<CryptoRateRecord> cryptoRecord = cryptoUpdate.fetchAndParseCrypto();
                if (!cryptoRecord.isEmpty()) {
                    database.updateCryptoRates(cryptoRecord);
                }
                database.setMeta("last_crypto_update", today.toString());
            } catch (Exception e) {
                log.error("Failed to update crypto rates: {}", e.getMessage());
            }
        }
    }

}
