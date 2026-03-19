package com.fetchrate.update;

import com.fetchrate.core.CurrencyClassifier;
import com.fetchrate.core.FiatRateRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FiatRateParserTest {

    private FiatRateParser parser;

    @BeforeEach
    void setUp() {
        parser = new FiatRateParser(new CurrencyClassifier());
    }

    @Test
    void parseFiat_returnsCorrectRecordsFromValidXml() {
        String xml = """
                <Cube>
                <Cube time="2024-01-15">
                <Cube currency="USD" rate="1.0942"/>
                <Cube currency="GBP" rate="0.8591"/>
                </Cube>
                </Cube>
                """;

        List<FiatRateRecord> records = parser.parseFiat(xml);

        assertEquals(2, records.size());
        FiatRateRecord usd = records.stream()
                .filter(r -> r.currency().equals("USD"))
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDate.of(2024, 1, 15), usd.date());
        assertEquals(new BigDecimal("1.0942"), usd.rate());
    }

    @Test
    void parseFiat_returnsEmptyListForEmptyInput() {
        List<FiatRateRecord> records = parser.parseFiat("");
        assertTrue(records.isEmpty());
    }

    @Test
    void parseFiat_ignoresUnknownCurrencies() {
        String xml = """
                <Cube>
                <Cube time="2024-01-15">
                <Cube currency="XYZ" rate="1.23"/>
                </Cube>
                </Cube>
                """;

        List<FiatRateRecord> records = parser.parseFiat(xml);
        assertTrue(records.isEmpty());
    }

    @Test
    void parseFiat_parsesMultipleDates() {
        String xml = """
                <Cube>
                <Cube time="2024-01-15">
                <Cube currency="USD" rate="1.0942"/>
                </Cube>
                <Cube time="2024-01-16">
                <Cube currency="USD" rate="1.0900"/>
                </Cube>
                </Cube>
                """;

        List<FiatRateRecord> records = parser.parseFiat(xml);
        assertEquals(2, records.size());
    }
}
