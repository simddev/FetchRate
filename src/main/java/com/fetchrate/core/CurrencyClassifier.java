package com.fetchrate.core;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

/**
 * This class serves to help identify if the user's query is fiat or cryptocurrency.
 * This later on helps with the business logic.
 */
@Component
public class CurrencyClassifier {

    private static final Map<String, String> FIAT_NAMES = Map.ofEntries(
            Map.entry("USD", "US Dollar"),
            Map.entry("JPY", "Japanese Yen"),
            Map.entry("BGN", "Bulgarian Lev"),
            Map.entry("CZK", "Czech Koruna"),
            Map.entry("DKK", "Danish Krone"),
            Map.entry("GBP", "Pound Sterling"),
            Map.entry("HUF", "Hungarian Forint"),
            Map.entry("PLN", "Polish Zloty"),
            Map.entry("RON", "Romanian Leu"),
            Map.entry("SEK", "Swedish Krona"),
            Map.entry("CHF", "Swiss Franc"),
            Map.entry("ISK", "Icelandic Krona"),
            Map.entry("NOK", "Norwegian Krone"),
            Map.entry("TRY", "Turkish Lira"),
            Map.entry("AUD", "Australian Dollar"),
            Map.entry("BRL", "Brazilian Real"),
            Map.entry("CAD", "Canadian Dollar"),
            Map.entry("CNY", "Chinese Yuan Renminbi"),
            Map.entry("HKD", "Hong Kong Dollar"),
            Map.entry("IDR", "Indonesian Rupiah"),
            Map.entry("ILS", "Israeli New Shekel"),
            Map.entry("INR", "Indian Rupee"),
            Map.entry("KRW", "South Korean Won"),
            Map.entry("MXN", "Mexican Peso"),
            Map.entry("MYR", "Malaysian Ringgit"),
            Map.entry("NZD", "New Zealand Dollar"),
            Map.entry("PHP", "Philippine Peso"),
            Map.entry("SGD", "Singapore Dollar"),
            Map.entry("THB", "Thai Baht"),
            Map.entry("ZAR", "South African Rand")
    );

    private static final Map<String, String> CRYPTO_NAMES = Map.of(
            "BTC", "Bitcoin",
            "ETH", "Ethereum",
            "LTC", "Litecoin",
            "DOGE", "Dogecoin",
            "SOL", "Solana",
            "USDT", "Tether"
    );

    public Map<String, String> getCurrencyNames() {
        Map<String, String> all = new java.util.HashMap<>(FIAT_NAMES);
        all.putAll(CRYPTO_NAMES);
        all.put("EUR", "Euro");
        return all;
    }

    public Set<String> getSupportedFiats() {
        return FIAT_NAMES.keySet();
    }

    public boolean isFiat(String symbol) {
        return FIAT_NAMES.containsKey(symbol);
    }

    public boolean isCrypto(String symbol) {
        return !isFiat(symbol);
    }
}
