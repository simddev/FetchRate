package com.fetchrate.persistence;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.core.QueryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateDatabaseTest {

    private RateDatabase db;

    @BeforeEach
    void setUp() {
        var ds = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        db = new RateDatabase(new JdbcTemplate(ds));
        db.initSchema();
    }

    // --- meta ---

    @Test
    void getMeta_keyNotSet_returnsNull() {
        assertNull(db.getMeta("missing_key"));
    }

    @Test
    void setMeta_andGetMeta_roundTrips() {
        db.setMeta("some_key", "some_value");
        assertEquals("some_value", db.getMeta("some_key"));
    }

    @Test
    void setMeta_upsert_overwritesExistingValue() {
        db.setMeta("k", "v1");
        db.setMeta("k", "v2");
        assertEquals("v2", db.getMeta("k"));
    }

    @Test
    void getLastUpdate_noEntry_returnsNull() {
        assertNull(db.getLastUpdate());
    }

    @Test
    void getLastUpdate_afterSetMeta_returnsDate() {
        db.setMeta("last_update", "2024-06-01");
        assertEquals(LocalDate.of(2024, 6, 1), db.getLastUpdate());
    }

    // --- fiat rates ---

    @Test
    void updateFiatRates_andFindFiatRate_roundTrips() {
        var date = LocalDate.of(2024, 1, 15);
        db.updateFiatRates(List.of(new FiatRateRecord("USD", date, new BigDecimal("0.9250"))));

        var result = db.findFiatRate(new QueryRecord(BigDecimal.ONE, "USD", date));

        assertEquals("USD", result.currency());
        assertEquals(date, result.date());
        assertEquals(0, new BigDecimal("0.9250").compareTo(result.rate()));
    }

    @Test
    void updateFiatRates_upsert_overwritesExistingRate() {
        var date = LocalDate.of(2024, 1, 15);
        db.updateFiatRates(List.of(new FiatRateRecord("USD", date, new BigDecimal("1.0"))));
        db.updateFiatRates(List.of(new FiatRateRecord("USD", date, new BigDecimal("2.0"))));

        var result = db.findFiatRate(new QueryRecord(BigDecimal.ONE, "USD", date));

        assertEquals(0, new BigDecimal("2.0").compareTo(result.rate()));
    }

    @Test
    void findFiatRate_missing_throwsIllegalArgumentException() {
        var query = new QueryRecord(BigDecimal.ONE, "GBP", LocalDate.of(2024, 1, 15));
        assertThrows(IllegalArgumentException.class, () -> db.findFiatRate(query));
    }

    @Test
    void updateFiatRates_multipleRecords_allStoredCorrectly() {
        var date = LocalDate.of(2024, 1, 15);
        db.updateFiatRates(List.of(
                new FiatRateRecord("USD", date, new BigDecimal("0.92")),
                new FiatRateRecord("GBP", date, new BigDecimal("1.17"))
        ));

        assertEquals(0, new BigDecimal("0.92").compareTo(
                db.findFiatRate(new QueryRecord(BigDecimal.ONE, "USD", date)).rate()));
        assertEquals(0, new BigDecimal("1.17").compareTo(
                db.findFiatRate(new QueryRecord(BigDecimal.ONE, "GBP", date)).rate()));
    }

    // --- crypto rates ---

    @Test
    void updateCryptoRates_andFindCryptoRate_roundTrips() {
        var date = LocalDate.of(2024, 1, 15);
        db.updateCryptoRates(List.of(new CryptoRateRecord("BTC", date, new BigDecimal("42000.00"))));

        var result = db.findCryptoRate(new QueryRecord(BigDecimal.ONE, "BTC", date));

        assertEquals("BTC", result.symbol());
        assertEquals(date, result.date());
        assertEquals(0, new BigDecimal("42000.00").compareTo(result.rate()));
    }

    @Test
    void updateCryptoRates_upsert_overwritesExistingRate() {
        var date = LocalDate.of(2024, 1, 15);
        db.updateCryptoRates(List.of(new CryptoRateRecord("ETH", date, new BigDecimal("100.0"))));
        db.updateCryptoRates(List.of(new CryptoRateRecord("ETH", date, new BigDecimal("200.0"))));

        var result = db.findCryptoRate(new QueryRecord(BigDecimal.ONE, "ETH", date));

        assertEquals(0, new BigDecimal("200.0").compareTo(result.rate()));
    }

    @Test
    void findCryptoRate_missing_throwsIllegalArgumentException() {
        var query = new QueryRecord(BigDecimal.ONE, "XRP", LocalDate.of(2024, 1, 15));
        assertThrows(IllegalArgumentException.class, () -> db.findCryptoRate(query));
    }

    @Test
    void updateCryptoRates_differentDates_storedIndependently() {
        var date1 = LocalDate.of(2024, 1, 14);
        var date2 = LocalDate.of(2024, 1, 15);
        db.updateCryptoRates(List.of(
                new CryptoRateRecord("BTC", date1, new BigDecimal("40000")),
                new CryptoRateRecord("BTC", date2, new BigDecimal("41000"))
        ));

        assertEquals(0, new BigDecimal("40000").compareTo(
                db.findCryptoRate(new QueryRecord(BigDecimal.ONE, "BTC", date1)).rate()));
        assertEquals(0, new BigDecimal("41000").compareTo(
                db.findCryptoRate(new QueryRecord(BigDecimal.ONE, "BTC", date2)).rate()));
    }

    // --- tracked symbols ---

    @Test
    void getTrackedSymbols_empty_returnsEmptyList() {
        assertTrue(db.getTrackedSymbols().isEmpty());
    }

    @Test
    void addTrackedSymbol_andGetTrackedSymbols_returnsSymbol() {
        db.addTrackedSymbol("XRP");
        assertEquals(List.of("XRP"), db.getTrackedSymbols());
    }

    @Test
    void addTrackedSymbol_duplicate_isIgnored() {
        db.addTrackedSymbol("BTC");
        db.addTrackedSymbol("BTC");
        assertEquals(1, db.getTrackedSymbols().size());
    }

    @Test
    void removeTrackedSymbol_removesCorrectSymbol() {
        db.addTrackedSymbol("BTC");
        db.addTrackedSymbol("ETH");
        db.removeTrackedSymbol("BTC");

        List<String> symbols = db.getTrackedSymbols();
        assertFalse(symbols.contains("BTC"));
        assertTrue(symbols.contains("ETH"));
    }

    @Test
    void removeTrackedSymbol_notPresent_doesNothing() {
        db.addTrackedSymbol("BTC");
        db.removeTrackedSymbol("XRP");
        assertEquals(List.of("BTC"), db.getTrackedSymbols());
    }

    @Test
    void addTrackedSymbol_multipleSymbols_allReturnedInOrder() {
        db.addTrackedSymbol("BTC");
        db.addTrackedSymbol("ETH");
        db.addTrackedSymbol("XRP");

        List<String> symbols = db.getTrackedSymbols();
        assertTrue(symbols.containsAll(List.of("BTC", "ETH", "XRP")));
        assertEquals(3, symbols.size());
    }
}
