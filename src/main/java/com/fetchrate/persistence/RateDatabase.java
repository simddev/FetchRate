package com.fetchrate.persistence;

import com.fetchrate.core.ExchangeRateRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class RateDatabase {

    private final JdbcTemplate jdbc;

    public RateDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // run once at startup (we’ll call this from your startup flow)
    public void initSchema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS fiat_rates (
                date TEXT NOT NULL,
                currency TEXT NOT NULL,
                rate TEXT NOT NULL,
                PRIMARY KEY (date, currency)
            )
        """);
    }

    public void updateFiatRates(List<ExchangeRateRecord> records) {
        String sql = """
            INSERT INTO fiat_rates(date, currency, rate)
            VALUES (?, ?, ?)
            ON CONFLICT(date, currency) DO UPDATE SET rate = excluded.rate
        """;

        for (ExchangeRateRecord r : records) {
            jdbc.update(sql,
                    r.date().toString(),
                    r.currency(),
                    r.rate().toPlainString()
            );
        }
    }
}
