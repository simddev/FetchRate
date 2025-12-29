package com.fetchrate.core;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * This class serves to help identify if the user's query is fiat or cryptocurrency.
 * This later on helps with the business logic.
 */
@Component
public class CurrencyClassifier {

    private static final Set<String> FIAT = Set.of(
            "USD","JPY","BGN","CZK","DKK","GBP","HUF","PLN","RON","SEK","CHF","ISK","NOK","TRY",
            "AUD","BRL","CAD","CNY","HKD","IDR","ILS","INR","KRW","MXN","MYR","NZD","PHP","SGD","THB","ZAR"
    );

    public boolean isFiat(String symbol) {
        return FIAT.contains(symbol);
    }

    public boolean isCrypto(String symbol) {
        return !isFiat(symbol);
    }
}
