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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void convert_fiatOnSaturday_throwsWithWeekendMessage() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        LocalDate saturday = LocalDate.of(2024, 1, 13); // known Saturday

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "USD", saturday)));

        assertTrue(ex.getMessage().contains("weekend"));
        assertTrue(ex.getMessage().contains("2024-01-12")); // previous Friday
        verifyNoInteractions(database);
    }

    @Test
    void convert_fiatOnSunday_throwsWithWeekendMessage() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        LocalDate sunday = LocalDate.of(2024, 1, 14); // known Sunday

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "USD", sunday)));

        assertTrue(ex.getMessage().contains("weekend"));
        assertTrue(ex.getMessage().contains("2024-01-12")); // previous Friday
        verifyNoInteractions(database);
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

    @Test
    void convert_cryptoNotInDb_lazyFetchSucceeds_returnsResult() {
        when(classifier.isSupported("XRP")).thenReturn(true);
        when(classifier.isFiat("XRP")).thenReturn(false);
        // First DB lookup misses, lazy fetch stores it, second lookup hits
        when(database.findCryptoRate(any()))
                .thenThrow(new IllegalArgumentException("No crypto rate found"))
                .thenReturn(new CryptoRateRecord("XRP", testDate, new BigDecimal("0.50")));
        when(cryptoUpdater.fetchAndParseSpecific(eq("XRP"), eq(testDate)))
                .thenReturn(List.of(new CryptoRateRecord("XRP", testDate, new BigDecimal("0.50"))));

        BigDecimal result = convertor.convert(new QueryRecord(new BigDecimal("100"), "XRP", testDate));

        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void convert_cryptoNotInDb_lazyFetchReturnsNothing_rethrowsOriginalError() {
        when(classifier.isSupported("XRP")).thenReturn(true);
        when(classifier.isFiat("XRP")).thenReturn(false);
        when(database.findCryptoRate(any()))
                .thenThrow(new IllegalArgumentException("No crypto rate found for XRP"));
        when(cryptoUpdater.fetchAndParseSpecific(eq("XRP"), eq(testDate)))
                .thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "XRP", testDate)));

        assertTrue(ex.getMessage().contains("XRP"));
    }
}
