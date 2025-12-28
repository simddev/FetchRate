package com.fetchrate.persistence;

import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.core.QueryRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * This class serves to create, update and query the database in FetchRate/data.
 */
@Repository
public class RateDatabase {

    private final JdbcTemplate jdbc;

    public RateDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * This method uses SQL through a SQLite driver to create a table in case it does not exist.
     */
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

    /**
     * This method updates the fiat rate table.
     *
     * @param records Takes the ArrayList of ExchangeRateRecords.
     */
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

    /**
     * This method queries for the given date and currency.
     *
     * @param query The Query Record holding the desired date and currency.
     * @return Returns a ExchangeRateRecord single instance.
     */

    public ExchangeRateRecord findFiatRate(QueryRecord query) {
        String sql = """
                SELECT currency, date, rate
                FROM fiat_rates
                WHERE date = ? AND currency = ?
                """;

        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new ExchangeRateRecord(
                            rs.getString("currency"),
                            LocalDate.parse(rs.getString("date")),
                            new BigDecimal(rs.getString("rate"))
                    ),
                    query.date().toString(),
                    query.currency()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No rate found for " + query.currency() + " on " + query.date());
        }
    }


    /**
     * This method just finds the latest update date in the database.
     * @return A LocalDate, which is the most up-to-date one.
     */
    public LocalDate findLatestFiatDate() {
        String sql = "SELECT MAX(date) AS max_date FROM fiat_rates";
        String maxDate = jdbc.queryForObject(sql, String.class);
        if (maxDate == null) return null;
        return LocalDate.parse(maxDate);
    }

}


