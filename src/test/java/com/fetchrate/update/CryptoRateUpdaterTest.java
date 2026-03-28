package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.persistence.RateDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoRateUpdaterTest {

    @Mock
    private CryptoRateFetcher fetcher;
    @Mock
    private CryptoRateParser parser;
    @Mock
    private RateDatabase database;

    @InjectMocks
    private CryptoRateUpdater updater;

    // --- getEffectiveSymbols ---

    @Test
    void getEffectiveSymbols_emptyDb_returnsDefaults() {
        when(database.getTrackedSymbols()).thenReturn(List.of());

        assertEquals(CryptoRateUpdater.DEFAULT_SYMBOLS, updater.getEffectiveSymbols());
    }

    @Test
    void getEffectiveSymbols_customList_returnsCustomList() {
        when(database.getTrackedSymbols()).thenReturn(List.of("XRP", "ADA"));

        assertEquals(List.of("XRP", "ADA"), updater.getEffectiveSymbols());
    }

    // --- isCustomized ---

    @Test
    void isCustomized_emptyDb_returnsFalse() {
        when(database.getTrackedSymbols()).thenReturn(List.of());

        assertFalse(updater.isCustomized());
    }

    @Test
    void isCustomized_withSymbols_returnsTrue() {
        when(database.getTrackedSymbols()).thenReturn(List.of("BTC"));

        assertTrue(updater.isCustomized());
    }

    // --- addTrackedSymbol (seeding behavior) ---

    @Test
    void addTrackedSymbol_emptyDb_seedsDefaultsFirst() {
        when(database.getTrackedSymbols()).thenReturn(List.of());

        updater.addTrackedSymbol("XRP");

        // All defaults must be seeded before the new symbol
        for (String sym : CryptoRateUpdater.DEFAULT_SYMBOLS) {
            verify(database).addTrackedSymbol(sym);
        }
        verify(database).addTrackedSymbol("XRP");
    }

    @Test
    void addTrackedSymbol_customListExists_doesNotSeedDefaults() {
        when(database.getTrackedSymbols()).thenReturn(List.of("BTC", "ETH"));

        updater.addTrackedSymbol("XRP");

        // Only the new symbol should be added
        verify(database, times(1)).addTrackedSymbol("XRP");
        verify(database, never()).addTrackedSymbol("LTC");
        verify(database, never()).addTrackedSymbol("DOGE");
    }

    // --- removeTrackedSymbol (seeding behavior) ---

    @Test
    void removeTrackedSymbol_emptyDb_seedsDefaultsFirst() {
        when(database.getTrackedSymbols()).thenReturn(List.of());

        updater.removeTrackedSymbol("DOGE");

        for (String sym : CryptoRateUpdater.DEFAULT_SYMBOLS) {
            verify(database).addTrackedSymbol(sym);
        }
        verify(database).removeTrackedSymbol("DOGE");
    }

    @Test
    void removeTrackedSymbol_customListExists_doesNotSeedDefaults() {
        when(database.getTrackedSymbols()).thenReturn(List.of("BTC", "ETH", "DOGE"));

        updater.removeTrackedSymbol("DOGE");

        verify(database, never()).addTrackedSymbol(any());
        verify(database).removeTrackedSymbol("DOGE");
    }

    // --- fetchAndParseSpecific ---

    @Test
    void fetchAndParseSpecific_noApiKey_returnsEmptyList() {
        when(fetcher.isApiKeyAvailable()).thenReturn(false);

        List<CryptoRateRecord> result = updater.fetchAndParseSpecific("BTC", LocalDate.of(2024, 1, 15));

        assertTrue(result.isEmpty());
        verifyNoInteractions(parser);
    }

    @Test
    void fetchAndParseSpecific_apiKeyAvailable_returnsRecords() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        CryptoRateRecord record = new CryptoRateRecord("BTC", date, new BigDecimal("42000"));
        when(fetcher.isApiKeyAvailable()).thenReturn(true);
        when(fetcher.fetchFromProvider(eq("BTC"), any(), any())).thenReturn("{json}");
        when(parser.parseProviderResponse("BTC", "{json}")).thenReturn(List.of(record));

        List<CryptoRateRecord> result = updater.fetchAndParseSpecific("BTC", date);

        assertEquals(1, result.size());
        assertEquals("BTC", result.get(0).symbol());
    }

    @Test
    void fetchAndParseSpecific_fetchThrows_returnsEmptyList() {
        when(fetcher.isApiKeyAvailable()).thenReturn(true);
        when(fetcher.fetchFromProvider(any(), any(), any())).thenThrow(new RuntimeException("API down"));

        List<CryptoRateRecord> result = updater.fetchAndParseSpecific("BTC", LocalDate.of(2024, 1, 15));

        assertTrue(result.isEmpty());
    }

    // --- fetchAndParseCrypto ---

    @Test
    void fetchAndParseCrypto_noApiKeyNoCsv_returnsEmptyList() {
        when(fetcher.fetchAllCsv()).thenReturn(java.util.Map.of());
        when(fetcher.isApiKeyAvailable()).thenReturn(false);

        assertTrue(updater.fetchAndParseCrypto().isEmpty());
    }

    @Test
    void fetchAndParseCrypto_apiKeyAvailable_usesEffectiveSymbols() {
        when(fetcher.fetchAllCsv()).thenReturn(java.util.Map.of());
        when(fetcher.isApiKeyAvailable()).thenReturn(true);
        when(database.getTrackedSymbols()).thenReturn(List.of("XRP", "ADA"));
        when(fetcher.fetchFromProvider(any(), any(), any())).thenReturn("{json}");
        when(parser.parseProviderResponse(any(), any())).thenReturn(List.of());

        updater.fetchAndParseCrypto();

        verify(fetcher).fetchFromProvider(eq("XRP"), any(), any());
        verify(fetcher).fetchFromProvider(eq("ADA"), any(), any());
        verify(fetcher, never()).fetchFromProvider(eq("BTC"), any(), any());
    }

    @Test
    void fetchAndParseCrypto_oneSymbolFails_othersStillProcessed() {
        when(fetcher.fetchAllCsv()).thenReturn(java.util.Map.of());
        when(fetcher.isApiKeyAvailable()).thenReturn(true);
        when(database.getTrackedSymbols()).thenReturn(List.of());
        // BTC throws, ETH succeeds
        when(fetcher.fetchFromProvider(eq("BTC"), any(), any())).thenThrow(new RuntimeException("fail"));
        when(fetcher.fetchFromProvider(eq("ETH"), any(), any())).thenReturn("{json}");
        when(parser.parseProviderResponse(eq("ETH"), any())).thenReturn(List.of(
                new CryptoRateRecord("ETH", LocalDate.now(), new BigDecimal("2000"))
        ));

        List<CryptoRateRecord> result = updater.fetchAndParseCrypto();

        // ETH record still present despite BTC failure
        assertEquals(1, result.size());
        assertEquals("ETH", result.get(0).symbol());
    }
}
