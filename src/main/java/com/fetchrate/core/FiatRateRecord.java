package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiatRateRecord(
        String currency,
        LocalDate date,
        BigDecimal rate
) {
}

