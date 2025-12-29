package com.fetchrate.persistence;

import com.fetchrate.core.ExchangeRateRecord;
import com.fetchrate.core.QueryRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * This class serves to create, update and query the database in /data.
 */
@Repository
public class RateDatabase {

    private final JdbcTemplate jdbc;

    public RateDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * This method uses SQL through a SQLite driver to create a table in case it does not exist.
     * <p>
     * It also creates a small meta table which keeps track of the last update date.
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

        jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                """);

    }

    /**
     * This method updates the fiat rate table.
     *
     * @param records Takes the ArrayList of ExchangeRateRecords.
     */
    @Transactional
    public void updateFiatRates(List<ExchangeRateRecord> records) {

        String sql = """
                    INSERT INTO fiat_rates(date, currency, rate)
                    VALUES (?, ?, ?)
                    ON CONFLICT(date, currency) DO UPDATE SET rate = excluded.rate
                """;

        System.out.println("Updating database, please wait...");

        jdbc.batchUpdate(
                sql,
                records,
                2000, // batch size
                (ps, r) -> {
                    ps.setString(1, r.date().toString());
                    ps.setString(2, r.currency());
                    ps.setString(3, r.rate().toPlainString());
                });

        setMeta("last_fiat_update", LocalDate.now().toString());

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
     *
     * @return A LocalDate, which is the most up-to-date one.
     */
    public LocalDate getLastFiatUpdate() {
        String v = getMeta("last_fiat_update");
        return (v == null) ? null : LocalDate.parse(v);
    }

    /**
     * Getter for the meta.
     *
     * @param key Last update key.
     * @return Actual Date at key.
     */
    public String getMeta(String key) {
        try {
            return jdbc.queryForObject(
                    "SELECT value FROM meta WHERE key = ?",
                    String.class,
                    key
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Setter for the meta.
     *
     * @param key   The key.
     * @param value The date.
     */
    public void setMeta(String key, String value) {
        jdbc.update("""
                    INSERT INTO meta(key, value)
                    VALUES (?, ?)
                    ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """, key, value);
    }

}
