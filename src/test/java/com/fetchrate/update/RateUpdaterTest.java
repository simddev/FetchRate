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
        when(fiatUpdate.fetchAndParseFiat()).thenReturn(Collections.emptyList());
        when(cryptoUpdate.fetchAndParseCrypto()).thenReturn(Collections.emptyList());
        
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // Act
        rateUpdater.updateRates();

        // Assert
        String expectedMessage = "Crypto Database not created because of missing .csv files. Please put the appropriate .csv files in /data/crypto in order to update the Crypto Exchange Rate Database";
        assertTrue(errContent.toString().contains(expectedMessage));
        
        verify(database, never()).updateCryptoRates(anyList());
        verify(database).setMeta(eq("last_update"), anyString());
        
        // Cleanup
        System.setErr(System.err);
    }
}
