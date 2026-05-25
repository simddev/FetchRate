package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable value object representing a single conversion request.
 * Passed through the service layer from adapters to {@link Convertor} and {@link com.fetchrate.persistence.RateDatabase}.
 */
public record QueryRecord(
        BigDecimal amount,
        String currencySymbol,
        LocalDate date
) {
}
