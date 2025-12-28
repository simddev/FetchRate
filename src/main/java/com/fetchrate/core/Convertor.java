package com.fetchrate.core;

import com.fetchrate.persistence.RateDatabase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class Convertor {

    private final RateDatabase database;

    public Convertor(RateDatabase database) {
        this.database = database;
    }

    public BigDecimal convert(QueryRecord query) {
        BigDecimal rate;
        BigDecimal amount;


        ExchangeRateRecord record = database.findFiatRate(query);
        rate = record.rate();
        amount = query.amount();

        return rate.multiply(amount);

    }

}
