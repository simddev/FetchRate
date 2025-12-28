package com.fetchrate.adapters.cli;

import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Component
public class CommandLineInterface implements CommandLineRunner {

    private final RateUpdater rateUpdater;
    private final Convertor convertor;

    public CommandLineInterface(RateUpdater rateUpdater, Convertor convertor) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
    }

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

            // autoupdate runs with the query too (your requirement)


            if (!alreadyUpdatedToday()) {
                rateUpdater.updateRates();
            }


            QueryRecord query = new QueryRecord(amount, currency, date);
            BigDecimal result = convertor.convert(query);

            // Minimal JSON to stdout (as per bounty spec)
            System.out.println("{\"amount\":\"" + amount + "\",\"inputCurrency\":\"" + currency +
                    "\",\"date\":\"" + date + "\",\"inEuros\":\"" + result + "\"}");

            return;
        }

        printUsage();
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar fetchrate.jar convert --amount 100 --input-currency CZK --date YYYY-MM-DD");
    }

    private boolean alreadyUpdatedToday() {
        Path path = Paths.get("data/LastUpdateDate.txt");

        if (!Files.exists(path)) {
            return false;
        }

        try {
            String text = Files.readString(path).trim();
            LocalDate lastUpdate = LocalDate.parse(text);
            return lastUpdate.equals(LocalDate.now());
        } catch (Exception e) {
            // file corrupted or unreadable → force update
            return false;
        }

    }
}
