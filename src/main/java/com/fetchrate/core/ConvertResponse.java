package com.fetchrate.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Response body for EUR-output conversions, used by both the REST endpoint and the CLI.
 * Contains the original request details in {@code input} and the converted amount in {@code output.inEuro}.
 * Cross-currency responses (non-EUR output) use the {@link #crossOf} factory method.
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

    /**
     * Builds the JSON response body for cross-currency conversions (non-EUR output).
     * Used by both the REST endpoint and the CLI to ensure a consistent response shape.
     */
    public static Map<String, Object> crossOf(BigDecimal amount, String currency, LocalDate date,
                                              BigDecimal result, String outputSymbol) {
        var input = new LinkedHashMap<String, String>();
        input.put("amount", amount.toPlainString());
        input.put("currencySymbol", currency);
        input.put("date", date.toString());

        var output = new LinkedHashMap<String, String>();
        output.put("amount", result.toPlainString());
        output.put("currency", outputSymbol);

        var response = new LinkedHashMap<String, Object>();
        response.put("input", input);
        response.put("output", output);
        return response;
    }

}
