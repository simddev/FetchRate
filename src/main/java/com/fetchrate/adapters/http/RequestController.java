package com.fetchrate.adapters.http;

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
     * @param amount The amount.
     * @param inputCurrency The input currency.
     * @param date The date.
     * @return Returns a JSON including inEuro.
     */
    @GetMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("input_currency") String inputCurrency,
            @RequestParam("date") LocalDate date
    ) {
        String currency = inputCurrency.toUpperCase();

        // Same behavior like the CLI version.
        if (!rateUpdater.alreadyUpdatedToday()) {
            rateUpdater.updateRates();
        }


        QueryRecord query = new QueryRecord(amount, currency, date);


        try {
            BigDecimal inEuros = convertor.convert(query);

            // Returning a JSON.
            return ResponseEntity.ok(Map.of(
                    "amount", amount.toPlainString(),
                    "inputCurrency", currency,
                    "date", date.toString(),
                    "inEuros", inEuros.toPlainString()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "No data available for that input.",
                    "inputCurrency", currency,
                    "date", date.toString()
            ));

        }
    }

    /**
     * Simple health endpoint.
     * @return Status.
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

}
