package com.fetchrate.adapters.web;

import com.fetchrate.core.CurrencyClassifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Profile("http")
@Controller
public class WebController {

    private final CurrencyClassifier classifier;

    public WebController(CurrencyClassifier classifier) {
        this.classifier = classifier;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<String> currencies = classifier.getSupportedFiats().stream()
                .sorted()
                .toList();
        model.addAttribute("currencies", currencies);
        return "index";
    }
}
