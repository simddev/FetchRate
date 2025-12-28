package com.fetchrate.update;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This serves as the ignition of the whole application.
 * <p>
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

    /**
     * This is run automatically since it overrides the run method from the CommandLineRunner interface.
     * <p>
     * It takes us to updateRates from the RateUpdater class.
     *
     * @param args Boilerplate.
     */
    @Override
    public void run(String... args) {
        rateUpdater.updateRates();
    }
}
