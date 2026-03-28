package com.fetchrate.persistence;

import com.fetchrate.core.CryptoRateRecord;
import com.fetchrate.core.FiatRateRecord;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.core.RateNotFoundException;
import jakarta.annotation.PostConstruct;
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
 * Data-access layer for the local SQLite database stored in {@code data/}.
 * Manages four tables: {@code fiat_rates}, {@code crypto_rates}, {@code meta}, and {@code tracked_symbols}.
 * The schema is created automatically on startup via {@link #initSchema()}.
 * All bulk writes use UPSERT ({@code ON CONFLICT DO UPDATE}) to stay idempotent.
 */
@Repository
public class RateDatabase {

    private static final Logger log = LoggerFactory.getLogger(RateDatabase.class);

    private final JdbcTemplate jdbc;

    public RateDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates all tables and indexes on startup if they do not already exist.
     */
    @PostConstruct
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

        jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS tracked_symbols (
                        symbol TEXT PRIMARY KEY
                    )
                """);

        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_fiat_rates_date_currency ON fiat_rates(date, currency)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_crypto_rates_date_symbol ON crypto_rates(date, symbol)");

    }

    /**
     * Bulk-upserts fiat exchange rates into the database.
     * Existing rows for the same date/currency pair are overwritten.
     *
     * @param records The records to persist.
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
     * Looks up the fiat exchange rate for the currency and date in the given query.
     *
     * @param query The query containing the currency symbol and date.
     * @return The matching {@link FiatRateRecord}.
     * @throws RateNotFoundException if no rate is found for the given currency and date.
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
            throw new RateNotFoundException("No rate found for " + query.currencySymbol() + " on " + query.date());
        }
    }


    /**
     * Returns the date of the last successful rate update, or {@code null} if the database
     * has never been updated.
     */
    public LocalDate getLastUpdate() {
        String v = getMeta("last_update");
        return (v == null) ? null : LocalDate.parse(v);
    }

    /**
     * Returns the value stored in the {@code meta} table for the given key,
     * or {@code null} if the key does not exist.
     *
     * @param key The meta key (e.g., {@code "last_update"}, {@code "crypto_api_key"}).
     * @return The stored value, or {@code null}.
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
     * Inserts or updates a key/value pair in the {@code meta} table.
     *
     * @param key   The meta key.
     * @param value The value to store.
     */
    public void setMeta(String key, String value) {
        jdbc.update("""
                    INSERT INTO meta(key, value)
                    VALUES (?, ?)
                    ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """, key, value);
    }


    /**
     * Bulk-upserts cryptocurrency rates into the database.
     * Existing rows for the same date/symbol pair are overwritten.
     *
     * @param records The records to persist.
     */
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

    /**
     * Looks up the cryptocurrency rate for the symbol and date in the given query.
     *
     * @param query The query containing the coin symbol and date.
     * @return The matching {@link CryptoRateRecord}.
     * @throws RateNotFoundException if no rate is found for the given symbol and date.
     */
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
            throw new RateNotFoundException("No crypto rate found for " + query.currencySymbol() + " on " + query.date());
        }
    }

    /**
     * Returns all symbols currently in the {@code tracked_symbols} table, in insertion order.
     * Returns an empty list when no custom list has been configured (defaults are in effect).
     */
    public List<String> getTrackedSymbols() {
        return jdbc.queryForList("SELECT symbol FROM tracked_symbols", String.class);
    }

    /**
     * Adds a symbol to the tracked list. Does nothing if the symbol is already present.
     *
     * @param symbol The coin symbol to track (e.g., {@code "XRP"}).
     */
    public void addTrackedSymbol(String symbol) {
        jdbc.update("INSERT OR IGNORE INTO tracked_symbols(symbol) VALUES (?)", symbol);
    }

    /**
     * Removes a symbol from the tracked list. Does nothing if the symbol is not present.
     *
     * @param symbol The coin symbol to remove.
     */
    public void removeTrackedSymbol(String symbol) {
        jdbc.update("DELETE FROM tracked_symbols WHERE symbol = ?", symbol);
    }

}
