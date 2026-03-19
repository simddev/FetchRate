package com.fetchrate.core;

import com.fetchrate.persistence.RateDatabase;
import com.fetchrate.update.CryptoRateUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertorTest {

    @Mock
    private RateDatabase database;
    @Mock
    private CurrencyClassifier classifier;
    @Mock
    private CryptoRateUpdater cryptoUpdater;

    @InjectMocks
    private Convertor convertor;

    private final LocalDate testDate = LocalDate.of(2024, 1, 15);

    @Test
    void convert_eurInputReturnsSameAmount() {
        when(classifier.isSupported("EUR")).thenReturn(true);

        BigDecimal result = convertor.convert(new QueryRecord(new BigDecimal("100.00"), "EUR", testDate));

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convert_fiatCurrencyDividesByRate() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        when(database.findFiatRate(any())).thenReturn(
                new FiatRateRecord("USD", testDate, new BigDecimal("2.00"))
        );

        BigDecimal result = convertor.convert(new QueryRecord(new BigDecimal("200.00"), "USD", testDate));

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convert_unsupportedCurrencyThrowsException() {
        when(classifier.isSupported("FAKE")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "FAKE", testDate)));
    }

    @Test
    void convert_fiatRateNotFoundThrowsException() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        when(database.findFiatRate(any())).thenThrow(new IllegalArgumentException("No rate found"));

        assertThrows(IllegalArgumentException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "USD", testDate)));
    }

    @Test
    void convert_cryptoCurrencyMultipliesByRate() {
        when(classifier.isSupported("BTC")).thenReturn(true);
        when(classifier.isFiat("BTC")).thenReturn(false);
        when(database.findCryptoRate(any())).thenReturn(
                new CryptoRateRecord("BTC", testDate, new BigDecimal("40000.00"))
        );

        BigDecimal result = convertor.convert(new QueryRecord(new BigDecimal("2.00"), "BTC", testDate));

        assertEquals(new BigDecimal("80000.00"), result);
    }
}
