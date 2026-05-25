package com.fetchrate.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response body for EUR-output conversions, used by both the REST endpoint and the CLI.
 * Contains the original request details in {@code input} and the converted amount in {@code output.inEuro}.
 * Cross-currency responses (non-EUR output) use a separate {@code LinkedHashMap} structure built inline.
 */
@JsonPropertyOrder({"input", "output"})
public record ConvertResponse(Input input, Output output) {

    @JsonPropertyOrder({"amount", "currencySymbol", "date"})
    public record Input(
            String amount,
            @JsonProperty("currencySymbol")
            String currency,
            String date
    ) {
    }

    public record Output(@JsonProperty("inEuro") String inEuro) {

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
