package com.fetchrate.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonPropertyOrder({"input", "output"})
public record ConvertResponse(Input input, Output output) {

    @JsonPropertyOrder({"amount", "currencySymbol", "date"})
    public record Input(
            String amount,
            String currency,
            String date
    ) {
    }

    public record Output(@JsonProperty("EUR") String eur) {

        public static Output of(BigDecimal eur) {
            return new Output(eur.toPlainString());
        }

    }

    public static ConvertResponse of(BigDecimal amount, String currency, LocalDate date, BigDecimal eur) {

        return new ConvertResponse(
                new Input(amount.toPlainString(), currency, date.toString()),
                Output.of(eur)
        );

    }

}
