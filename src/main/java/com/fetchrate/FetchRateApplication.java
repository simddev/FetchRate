package com.fetchrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This serves as the entry point of the application employing the Spring framework.
 * It will initiate to run a method from the RateUpdateStarterRunner class,
 * which implements the CommandLineRunner interface, thus starting off the chain
 * which will lead to a running application.
 * @author Simon
 */
@SpringBootApplication
public class FetchRateApplication {

    public static void main(String[] args) {

        SpringApplication.run(FetchRateApplication.class, args);

    }

}
