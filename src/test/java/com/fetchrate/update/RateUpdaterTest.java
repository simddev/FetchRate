package com.fetchrate.update;

import com.fetchrate.persistence.RateDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateUpdaterTest {

    @Mock
    private CryptoRateUpdater cryptoUpdate;
    @Mock
    private FiatRateUpdater fiatUpdate;
    @Mock
    private RateDatabase database;

    @InjectMocks
    private RateUpdater rateUpdater;

    @Test
    void testUpdateRatesWithMissingCryptoFiles() {
        // Arrange
        when(database.getLastUpdate()).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenReturn(Collections.emptyList());
        when(cryptoUpdate.fetchAndParseCrypto()).thenReturn(Collections.emptyList());

        // Act
        rateUpdater.updateRates();

        // Assert
        verify(database, never()).updateCryptoRates(anyList());
        verify(database).setMeta(eq("last_update"), anyString());
    }

    @Test
    void updateRates_bothFail_doesNotSetLastUpdate() {
        when(database.getLastUpdate()).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenThrow(new RuntimeException("ECB down"));
        when(cryptoUpdate.fetchAndParseCrypto()).thenThrow(new RuntimeException("API down"));

        rateUpdater.updateRates();

        verify(database, never()).setMeta(eq("last_update"), anyString());
    }

    @Test
    void updateRates_onlyFiatSucceeds_setsLastUpdate() {
        when(database.getLastUpdate()).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenReturn(Collections.emptyList());
        when(cryptoUpdate.fetchAndParseCrypto()).thenThrow(new RuntimeException("API down"));

        rateUpdater.updateRates();

        verify(database).setMeta(eq("last_update"), anyString());
    }

    @Test
    void testUpdateRatesSkipsIfAlreadyUpdated() {
        // Arrange
        when(database.getLastUpdate()).thenReturn(LocalDate.now());

        // Act
        rateUpdater.updateRates();

        // Assert
        verify(fiatUpdate, never()).fetchAndParseFiat();
        verify(cryptoUpdate, never()).fetchAndParseCrypto();
        verify(database, never()).updateFiatRates(anyList());
        verify(database, never()).setMeta(anyString(), anyString());
    }
}
