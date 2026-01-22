package com.fetchrate.core;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class Convertor {

    private final RateDatabase database;
    private final CurrencyClassifier classifier;

    public Convertor(RateDatabase database, CurrencyClassifier classifier) {
        this.database = database;
        this.classifier = classifier;
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
            FiatRateRecord fiatRecord = database.findFiatRate(
                    new QueryRecord(amount, currencySymbol, query.date())
            );

            if (fiatRecord == null) {
                throw new IllegalArgumentException("No fiat rate found for " + currencySymbol + " on " + query.date());
            }

            // The ECB gives us 1 EUR = Amount Foreign Currency.
            return amount.divide(fiatRecord.rate(), 2, RoundingMode.HALF_UP);
        }

        // At this point it must be crypto
        CryptoRateRecord cryptoRecord = database.findCryptoRate(
                new QueryRecord(amount, currencySymbol, query.date())
        );

        if (cryptoRecord == null) {
            throw new IllegalArgumentException("No crypto rate found for " + currencySymbol + " on " + query.date());
        }

        // Crypto CSV give rate of Amount EUR per 1 coin, so we multiply here.
        return amount.multiply(cryptoRecord.rate()).setScale(2, RoundingMode.HALF_UP);

    }

}
