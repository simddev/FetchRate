package com.fetchrate.adapters.http;

import com.fetchrate.core.ConvertResponse;
import com.fetchrate.core.Convertor;
import com.fetchrate.core.CurrencyClassifier;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.core.RateNotFoundException;
import com.fetchrate.update.RateUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for the HTTP server profile.
 * Exposes a {@code /convert} endpoint for currency conversions
 * and a {@code /health} endpoint for status checks.
 */
@Profile("http")
@RestController
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);

    private final RateUpdater rateUpdater;
    private final Convertor convertor;
    private final CurrencyClassifier classifier;

    public RequestController(RateUpdater rateUpdater, Convertor convertor, CurrencyClassifier classifier) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
        this.classifier = classifier;
    }

    /**
     * Converts an amount in the given currency on the specified date.
     * Defaults to EUR output; pass {@code output_currency} to convert to a different fiat or crypto.
     * Triggers a database update if rates have not yet been fetched today.
     *
     * @param amountStr            The amount to convert. Accepts commas and underscores as thousand separators.
     * @param inputCurrency        The source currency symbol (e.g., {@code USD}, {@code BTC}).
     * @param dateStr              The date in {@code YYYY-MM-DD} format. Must not be in the future.
     * @param outputCurrencyParam  Optional target currency (e.g., {@code GBP}, {@code ETH}). Defaults to EUR.
     * @return 200 with a JSON body on success, 400 for invalid input, 404 if no rate is found, or 500 on error.
     */
    @GetMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestParam(value = "amount", required = false) String amountStr,
            @RequestParam(value = "input_currency", required = false) String inputCurrency,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "output_currency", required = false) String outputCurrencyParam
    ) {
        if (amountStr == null || amountStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount parameter is required."));
        }
        if (inputCurrency == null || inputCurrency.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Currency symbol must not be blank."));
        }
        if (dateStr == null || dateStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "date parameter is required."));
        }
        String currency = inputCurrency.strip().toUpperCase();

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use YYYY-MM-DD."));
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.replace(",", "").replace("_", ""));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount format."));
        }

        if (date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date cannot be in the future."));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount must be greater than zero."));
        }

        if (!rateUpdater.alreadyUpdatedToday()) {
            rateUpdater.updateRates();
        }

        String outputCurrency = (outputCurrencyParam != null) ? outputCurrencyParam.strip().toUpperCase() : null;
        QueryRecord query = new QueryRecord(amount, currency, date);

        try {
            if (outputCurrency != null && !"EUR".equals(outputCurrency)) {
                BigDecimal result = classifier.isSupportedOutputCurrency(outputCurrency)
                        ? convertor.convertTo(query, outputCurrency)
                        : convertor.convertToCrypto(query, outputCurrency);
                return ResponseEntity.ok(buildCrossResponse(amount, currency, date, result, outputCurrency));
            }
            BigDecimal inEuros = convertor.convert(query);
            return ResponseEntity.ok(ConvertResponse.of(amount, currency, date, inEuros));
        } catch (RateNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", e.getMessage(),
                    "input", Map.of(
                            "amount", amount.toPlainString(),
                            "currencySymbol", currency,
                            "date", date.toString()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "An unexpected error occurred."
            ));
        }
    }

    /** Returns {@code {"status": "ok"}} for uptime checks. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private LinkedHashMap<String, Object> buildCrossResponse(
            BigDecimal amount, String currency, LocalDate date,
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
