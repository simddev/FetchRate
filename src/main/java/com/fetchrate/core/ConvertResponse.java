package com.fetchrate.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JSON response body returned by the {@code /convert} endpoint.
 * Contains the original request details in {@code input} and the converted EUR amount in {@code output}.
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

    public record Output(String amount, String currency) {

        public static Output of(BigDecimal result, String currency) {
            return new Output(result.toPlainString(), currency);
        }

    }

    public static ConvertResponse of(BigDecimal amount, String currency, LocalDate date, BigDecimal result, String outputCurrency) {

        return new ConvertResponse(
                new Input(amount.toPlainString(), currency, date.toString()),
                Output.of(result, outputCurrency)
        );

    }

}
