package com.fetchrate.adapters.cli;

import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The main class for handling CLI queries.
 * <p>
 * It is run first in case the first argument is "convert".
 */
@Profile("cli")
@Component
public class CommandLineInterface implements CommandLineRunner {

    private final RateUpdater rateUpdater;
    private final Convertor convertor;

    public CommandLineInterface(RateUpdater rateUpdater, Convertor convertor) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
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
                } else if ("--input-currency".equals(a) && i + 1 < args.length) {
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
                BigDecimal result = convertor.convert(query);

                // Minimal JSON to stdout.
                System.out.println("{\"amount\":\"" + amount + "\",\"inputCurrency\":\"" + currency +
                        "\",\"date\":\"" + date + "\",\"inEuros\":\"" + result + "\"}");
            } catch (IllegalArgumentException e) {
                System.out.println("No data available for that input.");
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
        System.out.println("  java -jar fetchrate.jar convert --amount 100 --input-currency CZK --date YYYY-MM-DD");
    }

}
