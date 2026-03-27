package com.fetchrate.core;

import com.fetchrate.persistence.RateDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Converts a given amount in a foreign currency to EUR using rates stored in the database.
 * <p>
 * Fiat conversions use ECB daily exchange rates (EUR base). Crypto conversions use rates
 * fetched from CSV files or the configured crypto data provider API. If a crypto rate is missing for the
 * requested date, a lazy fetch is attempted before throwing.
 */
@Service
public class Convertor {

    private static final Logger log = LoggerFactory.getLogger(Convertor.class);

    private final RateDatabase database;
    private final CurrencyClassifier classifier;
    private final com.fetchrate.update.CryptoRateUpdater cryptoUpdater;

    public Convertor(RateDatabase database, CurrencyClassifier classifier, com.fetchrate.update.CryptoRateUpdater cryptoUpdater) {
        this.database = database;
        this.classifier = classifier;
        this.cryptoUpdater = cryptoUpdater;
    }

    /**
     * Converts the amount in the given currency to EUR for the requested date.
     * <p>
     * For fiat currencies, divides the amount by the ECB rate (1 EUR = N foreign units).
     * For crypto, multiplies the amount by the stored rate (1 coin = N EUR).
     * Weekends are rejected for fiat because the ECB does not publish rates on those days.
     *
     * @param query The query containing the amount, currency symbol, and date.
     * @return The converted amount in EUR, rounded to 2 decimal places.
     * @throws IllegalArgumentException if the currency is unsupported, the date falls on a
     *                                  weekend (for fiat), or no rate is found in the database.
     */
    public BigDecimal convert(QueryRecord query) {
        String currencySymbol = query.currencySymbol().toUpperCase();

        if (!classifier.isSupported(currencySymbol)) {
            throw new IllegalArgumentException("Unsupported currency: " + currencySymbol);
        }

        // Safety net if user enters EUR.
        if ("EUR".equals(currencySymbol)) {
            return query.amount().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal amount = query.amount();

        // Checking if the user entered a currency or a symbol.
        if (classifier.isFiat(currencySymbol)) {
            DayOfWeek dow = query.date().getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                LocalDate friday = (dow == DayOfWeek.SATURDAY)
                        ? query.date().minusDays(1)
                        : query.date().minusDays(2);
                throw new IllegalArgumentException(
                        "No ECB rate for " + query.date() + " (weekend or holiday). " +
                        "The ECB only publishes rates on business days — try " + friday + " (Friday)."
                );
            }

            FiatRateRecord fiatRecord = database.findFiatRate(
                    new QueryRecord(amount, currencySymbol, query.date())
            );

            // The ECB gives us 1 EUR = Amount Foreign Currency.
            return amount.divide(fiatRecord.rate(), 2, RoundingMode.HALF_UP);
        }

        // At this point it must be crypto
        CryptoRateRecord cryptoRecord;
        try {
            cryptoRecord = database.findCryptoRate(
                    new QueryRecord(amount, currencySymbol, query.date())
            );
        } catch (IllegalArgumentException e) {
            // Try lazy fetch if not found
            log.info("Rate not in database. Attempting to fetch {} for {}...", currencySymbol, query.date());
            List<CryptoRateRecord> fetched = cryptoUpdater.fetchAndParseSpecific(currencySymbol, query.date());
            if (!fetched.isEmpty()) {
                database.updateCryptoRates(fetched);
                cryptoRecord = database.findCryptoRate(new QueryRecord(amount, currencySymbol, query.date()));
            } else {
                throw e;
            }
        }

        // Crypto rate is stored as EUR per 1 coin, so we multiply here.
        return amount.multiply(cryptoRecord.rate()).setScale(2, RoundingMode.HALF_UP);
    }

}
