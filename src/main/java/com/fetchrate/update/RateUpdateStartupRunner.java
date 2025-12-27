package com.fetchrate.update;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RateUpdateStartupRunner implements CommandLineRunner {

    private final RateUpdater rateUpdater;

    public RateUpdateStartupRunner(RateUpdater rateUpdater) {
        this.rateUpdater = rateUpdater;
    }

    @Override
    public void run(String... args) {
        rateUpdater.updateRates();
    }
}
