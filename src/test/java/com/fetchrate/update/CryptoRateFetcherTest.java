package com.fetchrate.update;

import com.fetchrate.config.CryptoProviderConfig;
import com.fetchrate.persistence.RateDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoRateFetcherTest {

    @Mock
    private RateDatabase database;
    @Mock
    private CryptoProviderConfig config;

    private CryptoRateFetcher fetcher;

    private final LocalDate start = LocalDate.of(2024, 1, 14);
    private final LocalDate end = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        fetcher = new CryptoRateFetcher("data/crypto", config, database);
    }

    @Test
    void fetchFromProvider_noKeyAnywhere_throwsIllegalState() {
        when(database.getMeta("crypto_api_key")).thenReturn(null);
        when(config.getApiKey()).thenReturn("");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    void fetchFromProvider_bothNull_throwsIllegalState() {
        when(database.getMeta("crypto_api_key")).thenReturn(null);
        when(config.getApiKey()).thenReturn(null);

        assertThrows(IllegalStateException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));
    }

    @Test
    void fetchFromProvider_dbKeyPresent_usedWithoutCallingConfig() {
        when(database.getMeta("crypto_api_key")).thenReturn("db-key");
        when(config.getProviderUrl()).thenReturn("http://localhost:1");

        // Should throw RuntimeException (network/IO), not IllegalStateException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));
        assertFalse(ex instanceof IllegalStateException, "Should not throw 'not configured' — key was found in DB");

        // DB key was used directly; config.getApiKey() should never have been called
        verify(config, never()).getApiKey();
    }

    @Test
    void fetchFromProvider_dbKeyBlankFallsBackToConfig() {
        when(database.getMeta("crypto_api_key")).thenReturn("   ");
        when(config.getApiKey()).thenReturn("config-key");
        when(config.getProviderUrl()).thenReturn("http://localhost:1");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));
        assertFalse(ex instanceof IllegalStateException, "Should not throw 'not configured' — fell back to config key");

        verify(config).getApiKey();
    }

    @Test
    void fetchFromProvider_dbKeyEmptyStringFallsBackToConfig() {
        when(database.getMeta("crypto_api_key")).thenReturn("");
        when(config.getApiKey()).thenReturn("config-key");
        when(config.getProviderUrl()).thenReturn("http://localhost:1");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));
        assertFalse(ex instanceof IllegalStateException);
        verify(config).getApiKey();
    }

    // --- URL resolution ---

    @Test
    void fetchFromProvider_dbUrlPresent_usedWithoutCallingConfigUrl() {
        when(database.getMeta("crypto_api_key")).thenReturn("key");
        when(database.getMeta("crypto_provider_url")).thenReturn("http://localhost:1/custom");

        assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));

        verify(config, never()).getProviderUrl();
    }

    @Test
    void fetchFromProvider_dbUrlBlankFallsBackToConfigUrl() {
        when(database.getMeta("crypto_api_key")).thenReturn("key");
        when(database.getMeta("crypto_provider_url")).thenReturn("  ");
        when(config.getProviderUrl()).thenReturn("http://localhost:1");

        assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));

        verify(config).getProviderUrl();
    }

    @Test
    void fetchFromProvider_dbUrlNullFallsBackToConfigUrl() {
        when(database.getMeta("crypto_api_key")).thenReturn("key");
        when(database.getMeta("crypto_provider_url")).thenReturn(null);
        when(config.getProviderUrl()).thenReturn("http://localhost:1");

        assertThrows(RuntimeException.class, () ->
                fetcher.fetchFromProvider("BTC", start, end));

        verify(config).getProviderUrl();
    }

    // --- isApiKeyAvailable ---

    @Test
    void isApiKeyAvailable_dbKeyPresent_returnsTrue() {
        when(database.getMeta("crypto_api_key")).thenReturn("some-key");
        assertTrue(fetcher.isApiKeyAvailable());
        verify(config, never()).getApiKey();
    }

    @Test
    void isApiKeyAvailable_dbKeyBlankConfigKeyPresent_returnsTrue() {
        when(database.getMeta("crypto_api_key")).thenReturn("");
        when(config.getApiKey()).thenReturn("config-key");
        assertTrue(fetcher.isApiKeyAvailable());
    }

    @Test
    void isApiKeyAvailable_noKeyAnywhere_returnsFalse() {
        when(database.getMeta("crypto_api_key")).thenReturn(null);
        when(config.getApiKey()).thenReturn(null);
        assertFalse(fetcher.isApiKeyAvailable());
    }

    @Test
    void isApiKeyAvailable_dbKeyBlankConfigKeyBlank_returnsFalse() {
        when(database.getMeta("crypto_api_key")).thenReturn("  ");
        when(config.getApiKey()).thenReturn("  ");
        assertFalse(fetcher.isApiKeyAvailable());
    }
}
