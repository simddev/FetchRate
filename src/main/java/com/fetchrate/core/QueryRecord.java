package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QueryRecord(
        BigDecimal amount,
        String currencySymbol,
        LocalDate date
) {
}
