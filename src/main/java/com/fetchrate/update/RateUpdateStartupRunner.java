package com.fetchrate.update;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** This serves as the ignition of the whole application.
 *
 * It employs the Spring integrated CommandLineRunner interface,
 * which is automatically run once the Spring framework has successfully booted.
 * It starts off with the updateRates method from the RateUpdater class,
 * which sets the rest in motion.
 */
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
