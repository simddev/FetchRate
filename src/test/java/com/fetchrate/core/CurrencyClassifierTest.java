package com.fetchrate.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyClassifierTest {

    private CurrencyClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new CurrencyClassifier();
    }

    @Test
    void isFiat_returnsTrueForKnownFiatCurrencies() {
        assertTrue(classifier.isFiat("USD"));
        assertTrue(classifier.isFiat("PLN"));
        assertTrue(classifier.isFiat("GBP"));
    }

    @Test
    void isFiat_returnsFalseForCrypto() {
        assertFalse(classifier.isFiat("BTC"));
        assertFalse(classifier.isFiat("ETH"));
    }

    @Test
    void isCrypto_returnsTrueForKnownCryptoCurrencies() {
        assertTrue(classifier.isCrypto("BTC"));
        assertTrue(classifier.isCrypto("ETH"));
        assertTrue(classifier.isCrypto("DOGE"));
    }

    @Test
    void isCrypto_returnsFalseForFiat() {
        assertFalse(classifier.isCrypto("USD"));
        assertFalse(classifier.isCrypto("EUR"));
    }

    @Test
    void isSupported_returnsTrueForEUR() {
        assertTrue(classifier.isSupported("EUR"));
    }

    @Test
    void isSupported_returnsTrueForKnownFiatAndCrypto() {
        assertTrue(classifier.isSupported("USD"));
        assertTrue(classifier.isSupported("BTC"));
    }

    @Test
    void isSupported_returnsTrueForUnknownButValidSymbol() {
        assertTrue(classifier.isSupported("XRP"));
        assertTrue(classifier.isSupported("AB"));
    }

    @Test
    void isSupported_returnsFalseForInvalidSymbols() {
        assertFalse(classifier.isSupported("abc"));
        assertFalse(classifier.isSupported("A"));
        assertFalse(classifier.isSupported("ABCDEFGHIJK"));
        assertFalse(classifier.isSupported(""));
    }
}
