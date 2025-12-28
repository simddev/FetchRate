package com.fetchrate.core;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
public class Convertor {

    private final RateDatabase database;

    public Convertor(RateDatabase database) {
        this.database = database;
    }

    /**
     * A simple converting method, extracts the rate from the ExchangeRateRecord
     * for the given date, and applies it to the amount given by the QueryRecord.
     */
    public BigDecimal convert(QueryRecord query) {
        BigDecimal rate;
        BigDecimal amount;


        ExchangeRateRecord record = database.findFiatRate(query);
        rate = record.rate();
        amount = query.amount();

        return amount.divide(rate, 2, RoundingMode.HALF_UP);

    }

}
