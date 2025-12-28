package com.fetchrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This serves as the entry point of the application employing the Spring framework.
 * <p>
 * Once it initiates, a method from the RateUpdateStartupRunner class will automatically run,
 * since it implements the CommandLineRunner interface, which is a Spring feature.
 * @author Simon
 */
@SpringBootApplication
public class FetchRateApplication {

    public static void main(String[] args) {

        SpringApplication.run(FetchRateApplication.class, args);

    }

}
