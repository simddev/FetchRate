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
import static org.mockito.ArgumentMatchers.argThat;
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
        when(database.findFiatRate(any())).thenThrow(new RateNotFoundException("No rate found for USD on 2024-01-15"));

        assertThrows(RateNotFoundException.class, () ->
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
                .thenThrow(new RateNotFoundException("No crypto rate found for XRP on 2024-01-15"))
                .thenReturn(new CryptoRateRecord("XRP", testDate, new BigDecimal("0.50")));
        when(cryptoUpdater.fetchAndParseSpecific(eq("XRP"), eq(testDate)))
                .thenReturn(List.of(new CryptoRateRecord("XRP", testDate, new BigDecimal("0.50"))));

        BigDecimal result = convertor.convert(new QueryRecord(new BigDecimal("100"), "XRP", testDate));

        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void convertTo_eurOutput_returnsSameAsConvert() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("EUR")).thenReturn(true);
        when(database.findFiatRate(any())).thenReturn(
                new FiatRateRecord("USD", testDate, new BigDecimal("2.00"))
        );

        BigDecimal result = convertor.convertTo(new QueryRecord(new BigDecimal("200.00"), "USD", testDate), "EUR");

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convertTo_fiatOutput_appliesPivotRate() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("GBP")).thenReturn(true);
        when(database.findFiatRate(any())).thenReturn(
                new FiatRateRecord("USD", testDate, new BigDecimal("2.00"))
        );
        when(database.findFiatRateOnOrBefore(eq("GBP"), eq(testDate))).thenReturn(
                new FiatRateRecord("GBP", testDate, new BigDecimal("0.85"))
        );

        // 200 USD ÷ 2.00 = 100 EUR × 0.85 = 85 GBP
        BigDecimal result = convertor.convertTo(new QueryRecord(new BigDecimal("200.00"), "USD", testDate), "GBP");

        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    void convertTo_unsupportedOutputCurrency_throwsBeforeConversion() {
        when(classifier.isSupportedOutputCurrency("BTC")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                convertor.convertTo(new QueryRecord(new BigDecimal("100"), "USD", testDate), "BTC"));

        verifyNoInteractions(database);
    }

    @Test
    void convertToCrypto_dividesByOutputCryptoRate() {
        when(classifier.isSupported("BTC")).thenReturn(true);
        when(classifier.isFiat("BTC")).thenReturn(false);
        when(classifier.isFiat("ETH")).thenReturn(false);
        when(database.findCryptoRate(argThat(q -> q != null && "BTC".equals(q.currencySymbol())))).thenReturn(
                new CryptoRateRecord("BTC", testDate, new BigDecimal("40000.00"))
        );
        when(database.findCryptoRate(argThat(q -> q != null && "ETH".equals(q.currencySymbol())))).thenReturn(
                new CryptoRateRecord("ETH", testDate, new BigDecimal("2500.00"))
        );

        // 2 BTC × 40000 = 80000 EUR ÷ 2500 = 32 ETH
        BigDecimal result = convertor.convertToCrypto(
                new QueryRecord(new BigDecimal("2"), "BTC", testDate), "ETH");

        assertEquals(new BigDecimal("32.00000000"), result);
    }

    @Test
    void convertToCrypto_fiatOutputSymbol_throwsWithHint() {
        when(classifier.isFiat("USD")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                convertor.convertToCrypto(new QueryRecord(new BigDecimal("100"), "BTC", testDate), "USD"));

        assertTrue(ex.getMessage().contains("--to"));
        verifyNoInteractions(database);
    }

    @Test
    void convertToCrypto_eurOutputSymbol_throwsWithHint() {
        when(classifier.isFiat("EUR")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                convertor.convertToCrypto(new QueryRecord(new BigDecimal("100"), "BTC", testDate), "EUR"));

        assertTrue(ex.getMessage().contains("--to"));
        verifyNoInteractions(database);
    }

    @Test
    void convertToCrypto_outputCryptoNotInDb_lazyFetchSucceeds() {
        when(classifier.isSupported("USD")).thenReturn(true);
        when(classifier.isFiat("USD")).thenReturn(true);
        when(classifier.isFiat("SOL")).thenReturn(false);
        when(database.findFiatRate(any())).thenReturn(
                new FiatRateRecord("USD", testDate, new BigDecimal("1.00"))
        );
        when(database.findCryptoRate(any()))
                .thenThrow(new RateNotFoundException("No crypto rate found for SOL on 2024-01-15"))
                .thenReturn(new CryptoRateRecord("SOL", testDate, new BigDecimal("100.00")));
        when(cryptoUpdater.fetchAndParseSpecific(eq("SOL"), eq(testDate)))
                .thenReturn(List.of(new CryptoRateRecord("SOL", testDate, new BigDecimal("100.00"))));

        // 100 USD ÷ 1.00 = 100 EUR ÷ 100 = 1 SOL
        BigDecimal result = convertor.convertToCrypto(
                new QueryRecord(new BigDecimal("100"), "USD", testDate), "SOL");

        assertEquals(new BigDecimal("1.00000000"), result);
    }

    @Test
    void convertToCrypto_outputCryptoNotInDb_lazyFetchFails_rethrows() {
        when(classifier.isSupported("BTC")).thenReturn(true);
        when(classifier.isFiat("BTC")).thenReturn(false);
        when(classifier.isFiat("XRP")).thenReturn(false);
        when(database.findCryptoRate(argThat(q -> q != null && "BTC".equals(q.currencySymbol())))).thenReturn(
                new CryptoRateRecord("BTC", testDate, new BigDecimal("40000.00"))
        );
        when(database.findCryptoRate(argThat(q -> q != null && "XRP".equals(q.currencySymbol()))))
                .thenThrow(new RateNotFoundException("No crypto rate found for XRP on 2024-01-15"));
        when(cryptoUpdater.fetchAndParseSpecific(eq("XRP"), eq(testDate))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                convertor.convertToCrypto(new QueryRecord(new BigDecimal("1"), "BTC", testDate), "XRP"));
    }

    @Test
    void convert_cryptoNotInDb_lazyFetchReturnsNothing_rethrowsOriginalError() {
        when(classifier.isSupported("XRP")).thenReturn(true);
        when(classifier.isFiat("XRP")).thenReturn(false);
        when(database.findCryptoRate(any()))
                .thenThrow(new RateNotFoundException("No crypto rate found for XRP on 2024-01-15"));
        when(cryptoUpdater.fetchAndParseSpecific(eq("XRP"), eq(testDate)))
                .thenReturn(List.of());

        RateNotFoundException ex = assertThrows(RateNotFoundException.class, () ->
                convertor.convert(new QueryRecord(new BigDecimal("100"), "XRP", testDate)));

        assertTrue(ex.getMessage().contains("XRP"));
    }
}
