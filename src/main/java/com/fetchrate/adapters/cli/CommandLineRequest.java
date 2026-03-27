package com.fetchrate.adapters.cli;

import com.fetchrate.core.ConvertResponse;
import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.CryptoRateUpdater;
import com.fetchrate.update.RateUpdater;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;

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
    private final CryptoRateUpdater cryptoUpdater;

    public CommandLineRequest(RateUpdater rateUpdater, Convertor convertor, ObjectMapper objectMapper, CryptoRateUpdater cryptoUpdater) {
        this.rateUpdater = rateUpdater;
        this.convertor = convertor;
        this.objectMapper = objectMapper;
        this.cryptoUpdater = cryptoUpdater;
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

        if ("-h".equals(command) || "--help".equals(command)) {
            printHelp();
            return;
        }

        if ("config".equals(command)) {
            handleConfig(args);
            return;
        }

        if ("convert".equals(command)) {
            BigDecimal amount = null;
            String currency = null;
            LocalDate date = null;

            for (int i = 1; i < args.length; i++) {
                String a = args[i];

                if (("--amount".equals(a) || "-a".equals(a)) && i + 1 < args.length) {
                    String amountStr = args[++i];
                    try {
                        amount = new BigDecimal(amountStr.replace(",", "").replace("_", ""));
                    } catch (NumberFormatException e) {
                        printError("Invalid amount format: " + amountStr);
                        return;
                    }
                } else if (("--input-currency".equals(a) || "-c".equals(a)) && i + 1 < args.length) {
                    currency = args[++i].toUpperCase();
                } else if (("--date".equals(a) || "-d".equals(a)) && i + 1 < args.length) {
                    try {
                        date = LocalDate.parse(args[++i]);
                    } catch (Exception e) {
                        System.out.println("{\"error\":\"Invalid date format. Use YYYY-MM-DD.\"}");
                        return;
                    }
                }
            }

            if (amount == null || currency == null || date == null) {
                printUsage();
                return;
            }

            if (date.isAfter(LocalDate.now())) {
                System.out.println("{\"error\":\"Date cannot be in the future.\"}");
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("{\"error\":\"Amount must be greater than zero.\"}");
                return;
            }

            // Update runs in case it wasn't updated today.
            if (!rateUpdater.alreadyUpdatedToday()) {
                rateUpdater.updateRates();
            }


            QueryRecord query = new QueryRecord(amount, currency, date);


            try {
                BigDecimal inEuros = convertor.convert(query);

                ConvertResponse response = ConvertResponse.of(amount, currency, date, inEuros);

                System.out.println(objectMapper.writeValueAsString(response));

            } catch (Exception e) {
                printError(e.getMessage() != null ? e.getMessage() : "Conversion failed");
            }

            return;
        }

        printUsage();

    }

    private void handleConfig(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                System.out.println("Configure the crypto data provider and tracked symbol list:");
                System.out.println("  java -jar fetchrate.jar config --set-key YOUR_API_KEY   Save your crypto data provider API key");
                System.out.println("  java -jar fetchrate.jar config --set-url URL            Override the default data provider URL");
                System.out.println("  java -jar fetchrate.jar config --add-symbol XRP         Add a symbol to the daily update list");
                System.out.println("  java -jar fetchrate.jar config --remove-symbol DOGE     Remove a symbol from the daily update list");
                System.out.println("  java -jar fetchrate.jar config --list-symbols           Show the current tracked symbol list");
                return;
            }
            if ("--set-key".equals(args[i]) && i + 1 < args.length) {
                writeProperty("fetchrate.api-key", args[++i].trim(), "API key");
                return;
            }
            if ("--set-url".equals(args[i]) && i + 1 < args.length) {
                writeProperty("fetchrate.provider-url", args[++i].trim(), "Provider URL");
                return;
            }
            if ("--add-symbol".equals(args[i]) && i + 1 < args.length) {
                String sym = args[++i].trim().toUpperCase();
                if (!sym.matches("^[A-Z0-9]{2,10}$")) {
                    printError("Invalid symbol. Use 2–10 alphanumeric characters (e.g. BTC, XRP).");
                    return;
                }
                cryptoUpdater.addTrackedSymbol(sym);
                System.out.println("{\"status\":\"" + sym + " added to tracked symbols\"}");
                return;
            }
            if ("--remove-symbol".equals(args[i]) && i + 1 < args.length) {
                String sym = args[++i].trim().toUpperCase();
                cryptoUpdater.removeTrackedSymbol(sym);
                System.out.println("{\"status\":\"" + sym + " removed from tracked symbols\"}");
                return;
            }
            if ("--list-symbols".equals(args[i])) {
                try {
                    java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
                    out.put("symbols", cryptoUpdater.getEffectiveSymbols());
                    out.put("customized", cryptoUpdater.isCustomized());
                    System.out.println(objectMapper.writeValueAsString(out));
                } catch (Exception e) {
                    printError("Failed to list symbols");
                }
                return;
            }
        }
        System.out.println("Usage:");
        System.out.println("  java -jar fetchrate.jar config --set-key YOUR_API_KEY");
        System.out.println("  java -jar fetchrate.jar config --set-url https://your-provider/endpoint");
        System.out.println("  java -jar fetchrate.jar config --add-symbol XRP");
        System.out.println("  java -jar fetchrate.jar config --remove-symbol DOGE");
        System.out.println("  java -jar fetchrate.jar config --list-symbols");
    }

    private void writeProperty(String propertyKey, String value, String label) {
        if (value.isBlank()) {
            System.out.println("{\"error\":\"" + label + " must not be empty.\"}");
            return;
        }
        try {
            Path config = Path.of("fetchrate.properties");
            String entry = propertyKey + "=" + value + System.lineSeparator();
            if (Files.exists(config)) {
                String content = Files.readString(config);
                String escapedKey = propertyKey.replace(".", "\\.");
                if (content.matches("(?s).*(?m)^" + escapedKey + "=.*$.*")) {
                    content = content.replaceAll("(?m)^" + escapedKey + "=.*$", Matcher.quoteReplacement(entry.trim()));
                    Files.writeString(config, content);
                } else {
                    Files.writeString(config, entry, StandardOpenOption.APPEND);
                }
            } else {
                Files.writeString(config, entry);
            }
            System.out.println("{\"status\":\"" + label + " saved to fetchrate.properties\"}");
        } catch (IOException e) {
            System.out.println("{\"error\":\"Could not write fetchrate.properties: " + e.getMessage() + "\"}");
        }
    }

    private void printError(String message) {
        try {
            System.out.println(objectMapper.writeValueAsString(Map.of("error", message)));
        } catch (Exception e) {
            System.out.println("{\"error\":\"An unexpected error occurred\"}");
        }
    }

    /** Prints a short usage reminder used when a command is missing or malformed. */
    private void printUsage() {
        System.out.println("Usage: java -jar fetchrate.jar <command> [options]");
        System.out.println("Commands: convert, config, start_http_server");
        System.out.println("Run with -h or --help for full documentation.");
    }

    /** Prints the full help text covering all available commands and options. */
    private void printHelp() {
        System.out.println("FetchRate — currency to EUR converter");
        System.out.println();
        System.out.println("USAGE");
        System.out.println("  java -jar fetchrate.jar <command> [options]");
        System.out.println();
        System.out.println("COMMANDS");
        System.out.println("  convert                    Convert an amount to EUR");
        System.out.println("    -a, --amount <n>           Amount to convert (commas and underscores allowed as separators)");
        System.out.println("    -c, --input-currency <s>   Currency or crypto symbol (e.g. USD, BTC)");
        System.out.println("    -d, --date <YYYY-MM-DD>    Date of the exchange rate");
        System.out.println();
        System.out.println("  start_http_server     Start the HTTP server (default port: 8000)");
        System.out.println("    --port <n>           Listen on a custom port instead of 8000");
        System.out.println("                        Web UI: http://localhost:<port>/");
        System.out.println("                        API:    GET /convert?amount=N&input_currency=X&date=YYYY-MM-DD");
        System.out.println();
        System.out.println("  config                      Manage runtime configuration");
        System.out.println("    --set-key <key>            Save your crypto data provider API key");
        System.out.println("    --set-url <url>            Override the default crypto data provider URL");
        System.out.println("    --add-symbol <sym>         Add a symbol to the daily update list (e.g. XRP)");
        System.out.println("    --remove-symbol <sym>      Remove a symbol from the daily update list");
        System.out.println("    --list-symbols             Show current tracked symbols");
        System.out.println();
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("SUPPORTED CURRENCIES");
        System.out.println("  Fiat (ECB): USD, GBP, CHF, JPY, PLN, CZK, SEK, NOK, DKK, and more");
        System.out.println("  Crypto:     BTC, ETH, LTC, DOGE, SOL, USDT (and any symbol via configured provider)");
        System.out.println();
        System.out.println("EXAMPLES");
        System.out.println("  java -jar fetchrate.jar convert --amount 100 --input-currency USD --date 2024-01-15");
        System.out.println("  java -jar fetchrate.jar convert -a 0.5 -c BTC -d 2024-01-15");
        System.out.println("  java -jar fetchrate.jar start_http_server");
        System.out.println("  java -jar fetchrate.jar start_http_server --port 9090");
        System.out.println("  java -jar fetchrate.jar config --set-key YOUR_API_KEY");
    }

}
