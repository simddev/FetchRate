package com.fetchrate.adapters.web;

import com.fetchrate.core.CurrencyClassifier;
import com.fetchrate.update.CryptoRateUpdater;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Thymeleaf controller for the web UI.
 * Serves the main conversion page, passing the supported currency lists to the template.
 */
@Profile("http")
@Controller
public class WebController {

    private final CurrencyClassifier classifier;
    private final CryptoRateUpdater cryptoUpdater;

    public WebController(CurrencyClassifier classifier, CryptoRateUpdater cryptoUpdater) {
        this.classifier = classifier;
        this.cryptoUpdater = cryptoUpdater;
    }

    /**
     * Renders the main conversion page, injecting sorted lists of fiat and crypto symbols
     * and a name map for display in the currency selector.
     * The crypto list merges static known symbols with any user-configured tracked symbols.
     */
    @GetMapping("/")
    public String index(Model model) {
        Map<String, String> currencyNames = classifier.getCurrencyNames();

        Set<String> cryptoSymbols = new HashSet<>(currencyNames.keySet().stream()
                .filter(classifier::isCrypto)
                .toList());
        if (cryptoUpdater.isCustomized()) {
            cryptoSymbols.addAll(cryptoUpdater.getEffectiveSymbols());
        }

        Map<String, String> names = new HashMap<>(currencyNames);
        for (String sym : cryptoSymbols) {
            names.putIfAbsent(sym, sym);
        }

        model.addAttribute("fiats", classifier.getSupportedFiats().stream().sorted().toList());
        model.addAttribute("cryptos", cryptoSymbols.stream().sorted().toList());
        model.addAttribute("currencyNames", names);
        return "index";
    }
}
