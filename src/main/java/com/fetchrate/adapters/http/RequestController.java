package com.fetchrate.adapters.http;

import com.fetchrate.core.ConvertResponse;
import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for the HTTP server profile.
 * Exposes a {@code /convert} endpoint for currency-to-EUR conversions
 * and a {@code /health} endpoint for status checks.
 */
@Profile("http")
@RestController
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);

    private final RateUpdater rateUpdater;
    private final Convertor convertor;

    public RequestController(RateUpdater rateUpdater, Convertor convertor) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
    }


    /**
     * Converts an amount in the given currency to EUR on the specified date.
     * Triggers a database update if rates have not yet been fetched today.
     *
     * @param amountStr     The amount to convert. Accepts commas and underscores as thousand separators.
     * @param inputCurrency The source currency symbol (e.g., {@code USD}, {@code BTC}).
     * @param dateStr       The date in {@code YYYY-MM-DD} format. Must not be in the future.
     * @return 200 with a {@link com.fetchrate.core.ConvertResponse} JSON body on success,
     *         400 for invalid input, 404 if no rate is found, or 500 on an unexpected error.
     */
    @GetMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestParam("amount") String amountStr,
            @RequestParam("input_currency") String inputCurrency,
            @RequestParam("date") String dateStr
    ) {
        String currency = inputCurrency.toUpperCase();

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use YYYY-MM-DD."));
        }

        // Support for 100,000 and 100_000
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

        // Same behavior like the CLI version.
        if (!rateUpdater.alreadyUpdatedToday()) {
            rateUpdater.updateRates();
        }

        // Tries the convertor method, if successful formats it into a ConvertResponse record.
        // This ensures the client's format wish.
        try {
            BigDecimal inEuros = convertor.convert(new QueryRecord(amount, currency, date));
            return ResponseEntity.ok(
                    ConvertResponse.of(amount, currency, date, inEuros)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", e.getMessage(),
                    "input", Map.of(
                            "amount", amount.toPlainString(),
                            "currencySymbol", currency,
                            "date", date.toString()
                    )
            ));
        } catch (Exception e) {
            log.error("Unexpected error during conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "An unexpected error occurred."
            ));
        }
    }

    /**
     * Simple health endpoint for status check.
     * @return Status.
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

}
