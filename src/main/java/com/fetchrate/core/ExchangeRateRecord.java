package com.fetchrate.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateRecord(
        String currency,
        LocalDate date,
        BigDecimal rateInEur
) {}

