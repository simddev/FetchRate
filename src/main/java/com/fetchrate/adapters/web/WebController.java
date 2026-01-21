package com.fetchrate.adapters.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Profile("http")
@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
