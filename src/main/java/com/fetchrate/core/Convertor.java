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
     * A simple converting method, extracts the rate from the ExchangeRateRecord
     * for the given date, and applies it to the amount given by the QueryRecord.
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

        // Crypto CSV give rate of Amount EUR per 1 coin, so we multiply here.
        return amount.multiply(cryptoRecord.rate()).setScale(2, RoundingMode.HALF_UP);
    }

}
