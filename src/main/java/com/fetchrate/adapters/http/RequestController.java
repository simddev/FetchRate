package com.fetchrate.adapters.http;

import com.fetchrate.core.ConvertResponse;
import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * This is an adapter for the http server version of the application.
 * <p>
 * It hosts a server and responds to GET requests.
 */
@Profile("http")
@RestController
public class RequestController {

    private final RateUpdater rateUpdater;
    private final Convertor convertor;

    public RequestController(RateUpdater rateUpdater, Convertor convertor) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
    }


    /**
     * This is the response entity.
     * @param amountStr The amount as String.
     * @param inputCurrency The input currencySymbol.
     * @param dateStr The date as String.
     * @return Returns a JSON including inEuro.
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
                            "currency", currency,
                            "date", date.toString()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "An unexpected error occurred: " + e.getMessage()
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
