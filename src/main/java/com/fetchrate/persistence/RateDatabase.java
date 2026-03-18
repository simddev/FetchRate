package com.fetchrate.persistence;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.core.QueryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RateDatabase.class);

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

        jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS crypto_rates (
                        date TEXT NOT NULL,
                        symbol TEXT NOT NULL,
                        rate TEXT NOT NULL,
                        PRIMARY KEY (date, symbol)
                    )
                """);

        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_fiat_rates_date_currency ON fiat_rates(date, currency)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_crypto_rates_date_symbol ON crypto_rates(date, symbol)");

    }

    /**
     * This method updates the fiat rate table.
     *
     * @param records Takes the List of FiatRateRecords.
     */
    @Transactional
    public void updateFiatRates(List<FiatRateRecord> records) {

        String sql = """
                    INSERT INTO fiat_rates(date, currency, rate)
                    VALUES (?, ?, ?)
                    ON CONFLICT(date, currency) DO UPDATE SET rate = excluded.rate
                """;

        jdbc.batchUpdate(
                sql,
                records,
                2000, // batch size
                (ps, r) -> {
                    ps.setString(1, r.date().toString());
                    ps.setString(2, r.currency());
                    ps.setString(3, r.rate().toPlainString());
                });

    }

    /**
     * This method queries for the given date and currencySymbol.
     *
     * @param query The Query Record holding the desired date and currencySymbol.
     * @return Returns a ExchangeRateRecord single instance.
     */

    public FiatRateRecord findFiatRate(QueryRecord query) {
        String sql = """
                SELECT currency, date, rate
                FROM fiat_rates
                WHERE date = ? AND currency = ?
                """;

        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new FiatRateRecord(
                            rs.getString("currency"),
                            LocalDate.parse(rs.getString("date")),
                            new BigDecimal(rs.getString("rate"))
                    ),
                    query.date().toString(),
                    query.currencySymbol()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No rate found for " + query.currencySymbol() + " on " + query.date());
        }
    }


    /**
     * This method just finds the latest update date in the database.
     *
     * @return A LocalDate, which is the most up-to-date one.
     */
    public LocalDate getLastUpdate() {
        String v = getMeta("last_update");
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
        } catch (org.springframework.dao.DataAccessException e) {
            // Handle case where 'meta' table does not exist yet or other SQL issues
            if (e.getMessage() != null && e.getMessage().contains("meta")) {
                return null;
            }
            throw e;
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


    //Crypto Table and methods below

    @Transactional
    public void updateCryptoRates(List<CryptoRateRecord> records) {

        String sql = """
                    INSERT INTO crypto_rates(date, symbol, rate)
                    VALUES (?, ?, ?)
                    ON CONFLICT(date, symbol) DO UPDATE SET rate = excluded.rate
                """;

        log.info("Updating crypto database, please wait...");

        jdbc.batchUpdate(
                sql,
                records,
                2000,
                (ps, r) -> {
                    ps.setString(1, r.date().toString());
                    ps.setString(2, r.symbol());
                    ps.setString(3, r.rate().toPlainString());
                });

    }

    public CryptoRateRecord findCryptoRate(QueryRecord query) {
        String sql = """
                    SELECT symbol, date, rate
                    FROM crypto_rates
                    WHERE date = ? AND symbol = ?
                """;

        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new CryptoRateRecord(
                            rs.getString("symbol"),
                            LocalDate.parse(rs.getString("date")),
                            new BigDecimal(rs.getString("rate"))
                    ),
                    query.date().toString(),
                    query.currencySymbol()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("No crypto rate found for " + query.currencySymbol() + " on " + query.date());
        }
    }


}
