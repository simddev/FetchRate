package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CryptoRateParserTest {

    private CryptoRateParser parser;

    @BeforeEach
    void setUp() {
        parser = new CryptoRateParser(new ObjectMapper());
    }

    @Test
    void parseCrypto_returnsCorrectRecordsFromValidCsv() {
        String csv = "Start,End,Open,High,Low,Close,Volume\n" +
                "2024-01-14,2024-01-15,41000.00,43000.00,40000.00,42000.50,1000000";

        List<CryptoRateRecord> records = parser.parseCrypto("BTC", csv);

        assertEquals(1, records.size());
        assertEquals("BTC", records.get(0).symbol());
        assertEquals(LocalDate.of(2024, 1, 15), records.get(0).date());
        assertEquals(new BigDecimal("42000.50"), records.get(0).rate());
    }

    @Test
    void parseCrypto_returnsEmptyListForHeaderOnlyCsv() {
        String csv = "Start,End,Open,High,Low,Close,Volume";
        List<CryptoRateRecord> records = parser.parseCrypto("BTC", csv);
        assertTrue(records.isEmpty());
    }

    @Test
    void parseCrypto_returnsEmptyListForEmptyInput() {
        List<CryptoRateRecord> records = parser.parseCrypto("BTC", "");
        assertTrue(records.isEmpty());
    }

    @Test
    void parseCrypto_normalizesSymbolToUpperCase() {
        String csv = "Start,End,Open,High,Low,Close,Volume\n" +
                "2024-01-14,2024-01-15,0.00,0.00,0.00,1.50,0";

        List<CryptoRateRecord> records = parser.parseCrypto("eth", csv);

        assertEquals(1, records.size());
        assertEquals("ETH", records.get(0).symbol());
    }

    @Test
    void parseLiveCoinWatch_returnsCorrectRecordsFromValidJson() {
        // 1705276800000 = 2024-01-15 00:00:00 UTC
        String json = "{\"code\":\"BTC\",\"history\":[{\"date\":1705276800000,\"rate\":42000.50}]}";

        List<CryptoRateRecord> records = parser.parseLiveCoinWatch("BTC", json);

        assertEquals(1, records.size());
        assertEquals("BTC", records.get(0).symbol());
        assertEquals(LocalDate.of(2024, 1, 15), records.get(0).date());
    }

    @Test
    void parseLiveCoinWatch_returnsEmptyListForBlankInput() {
        List<CryptoRateRecord> records = parser.parseLiveCoinWatch("BTC", "");
        assertTrue(records.isEmpty());
    }
}
