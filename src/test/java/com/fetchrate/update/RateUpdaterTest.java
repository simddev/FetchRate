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
    void updateRates_bothSucceed_setsBothKeys() {
        when(database.getMeta("last_fiat_update")).thenReturn(null);
        when(database.getMeta("last_crypto_update")).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenReturn(Collections.emptyList());
        when(cryptoUpdate.fetchAndParseCrypto()).thenReturn(Collections.emptyList());

        rateUpdater.updateRates();

        verify(database).setMeta(eq("last_fiat_update"), anyString());
        verify(database).setMeta(eq("last_crypto_update"), anyString());
    }

    @Test
    void updateRates_bothFail_setsNeitherKey() {
        when(database.getMeta("last_fiat_update")).thenReturn(null);
        when(database.getMeta("last_crypto_update")).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenThrow(new RuntimeException("ECB down"));
        when(cryptoUpdate.fetchAndParseCrypto()).thenThrow(new RuntimeException("API down"));

        rateUpdater.updateRates();

        verify(database, never()).setMeta(eq("last_fiat_update"), anyString());
        verify(database, never()).setMeta(eq("last_crypto_update"), anyString());
    }

    @Test
    void updateRates_onlyFiatSucceeds_setsFiatKeyOnly() {
        when(database.getMeta("last_fiat_update")).thenReturn(null);
        when(database.getMeta("last_crypto_update")).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenReturn(Collections.emptyList());
        when(cryptoUpdate.fetchAndParseCrypto()).thenThrow(new RuntimeException("API down"));

        rateUpdater.updateRates();

        verify(database).setMeta(eq("last_fiat_update"), anyString());
        verify(database, never()).setMeta(eq("last_crypto_update"), anyString());
    }

    @Test
    void updateRates_onlyCryptoSucceeds_setsCryptoKeyOnly() {
        when(database.getMeta("last_fiat_update")).thenReturn(null);
        when(database.getMeta("last_crypto_update")).thenReturn(null);
        when(fiatUpdate.fetchAndParseFiat()).thenThrow(new RuntimeException("ECB down"));
        when(cryptoUpdate.fetchAndParseCrypto()).thenReturn(Collections.emptyList());

        rateUpdater.updateRates();

        verify(database, never()).setMeta(eq("last_fiat_update"), anyString());
        verify(database).setMeta(eq("last_crypto_update"), anyString());
    }

    @Test
    void updateRates_skipsIfBothAlreadyDoneToday() {
        String today = LocalDate.now().toString();
        when(database.getMeta("last_fiat_update")).thenReturn(today);
        when(database.getMeta("last_crypto_update")).thenReturn(today);

        rateUpdater.updateRates();

        verify(fiatUpdate, never()).fetchAndParseFiat();
        verify(cryptoUpdate, never()).fetchAndParseCrypto();
        verify(database, never()).updateFiatRates(anyList());
        verify(database, never()).setMeta(anyString(), anyString());
    }

    @Test
    void updateRates_fiatDoneButCryptoNot_onlyRunsCrypto() {
        String today = LocalDate.now().toString();
        when(database.getMeta("last_fiat_update")).thenReturn(today);
        when(database.getMeta("last_crypto_update")).thenReturn(null);
        when(cryptoUpdate.fetchAndParseCrypto()).thenReturn(Collections.emptyList());

        rateUpdater.updateRates();

        verify(fiatUpdate, never()).fetchAndParseFiat();
        verify(cryptoUpdate).fetchAndParseCrypto();
        verify(database, never()).setMeta(eq("last_fiat_update"), anyString());
        verify(database).setMeta(eq("last_crypto_update"), anyString());
    }

    @Test
    void alreadyUpdatedToday_bothDone_returnsTrue() {
        String today = LocalDate.now().toString();
        when(database.getMeta("last_fiat_update")).thenReturn(today);
        when(database.getMeta("last_crypto_update")).thenReturn(today);

        assertTrue(rateUpdater.alreadyUpdatedToday());
    }
}
