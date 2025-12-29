package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CryptoRateRecord(
        String symbol,
        LocalDate date,
        BigDecimal rate
) {
}

