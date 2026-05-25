package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable value object holding a single cryptocurrency exchange rate row.
 * {@code rate} is expressed as EUR per 1 coin (e.g. 1 BTC = 42000 EUR).
 */
public record CryptoRateRecord(
        String symbol,
        LocalDate date,
        BigDecimal rate
) {
}

