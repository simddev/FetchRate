package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable value object holding a single ECB fiat exchange rate row.
 * {@code rate} is expressed as the ECB convention: 1 EUR = {@code rate} units of {@code currency}.
 */
public record FiatRateRecord(
        String currency,
        LocalDate date,
        BigDecimal rate
) {
}

