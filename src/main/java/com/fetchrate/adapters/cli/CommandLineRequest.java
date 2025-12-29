package com.fetchrate.adapters.cli;

import com.fetchrate.core.ConvertResponse;
import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The main class for handling CLI queries.
 * <p>
 * It is run first in case the first argument is "convert".
 */
@Profile("cli")
@Component
public class CommandLineRequest implements CommandLineRunner {

    private final RateUpdater rateUpdater;
    private final Convertor convertor;
    private final ObjectMapper objectMapper;

    public CommandLineRequest(RateUpdater rateUpdater, Convertor convertor, ObjectMapper objectMapper) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs first and handles the conversion, checking last update date, updates if necessary,
     * returns a JSON in case the query was successful.
     *
     * @param args The command line arguments.
     */
    @Override
    public void run(String... args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];

        if ("convert".equals(command)) {
            BigDecimal amount = null;
            String currency = null;
            LocalDate date = null;

            for (int i = 1; i < args.length; i++) {
                String a = args[i];

                if ("--amount".equals(a) && i + 1 < args.length) {
                    amount = new BigDecimal(args[++i]);
                } else if ("--input-currencySymbol".equals(a) && i + 1 < args.length) {
                    currency = args[++i].toUpperCase();
                } else if ("--date".equals(a) && i + 1 < args.length) {
                    date = LocalDate.parse(args[++i]);
                }
            }

            if (amount == null || currency == null || date == null) {
                printUsage();
                return;
            }

            // Update runs in case it wasn't updated today.
            if (!rateUpdater.alreadyUpdatedToday()) {
                rateUpdater.updateRates();
            }


            QueryRecord query = new QueryRecord(amount, currency, date);


            try {
                BigDecimal inEuros = convertor.convert(query);
                ConvertResponse resp = ConvertResponse.of(amount, currency, date, inEuros);

                // This is the CLI equivalent of what Spring does automatically in the controller:
                System.out.println(objectMapper.writeValueAsString(resp));
            } catch (IllegalArgumentException e) {
                // You can also output a structured error JSON here if the client expects it.
                System.out.println("{\"error\":\"No data available for that input.\"}");
            }


            return;
        }

        printUsage();

    }

    /**
     * Shows usage to user in case of wrong format.
     */
    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar fetchrate.jar convert --amount 100 --input-currencySymbol CZK --date YYYY-MM-DD");
    }

}
