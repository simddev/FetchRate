package com.fetchrate.adapters.web;

import com.fetchrate.core.CurrencyClassifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Thymeleaf controller for the web UI.
 * Serves the main conversion page, passing the supported currency lists to the template.
 */
@Profile("http")
@Controller
public class WebController {

    private final CurrencyClassifier classifier;

    public WebController(CurrencyClassifier classifier) {
        this.classifier = classifier;
    }

    /**
     * Renders the main conversion page, injecting sorted lists of fiat and crypto symbols
     * and a name map for display in the currency selector.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("fiats", classifier.getSupportedFiats().stream().sorted().toList());
        model.addAttribute("cryptos", classifier.getCurrencyNames().keySet().stream()
                .filter(classifier::isCrypto)
                .sorted()
                .toList());
        model.addAttribute("currencyNames", classifier.getCurrencyNames());
        return "index";
    }
}
