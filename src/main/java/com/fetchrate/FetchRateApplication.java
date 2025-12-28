package com.fetchrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point for Spring Boot.
 * <p>
 * This will run first, check if the first argument is start_http_server.
 * <p>
 * If yes, it will run the server profile, if not, it will run a minimalist version,
 * <p>
 * to speed up the CLI responses.
 */
@SpringBootApplication
public class FetchRateApplication {

    public static void main(String[] args) {
        boolean startServer = args.length > 0 && "start_http_server".equals(args[0]);

        SpringApplication app = new SpringApplication(FetchRateApplication.class);

        // 1) Picks a profile, application-cli.properties or application-http.properties
        app.setAdditionalProfiles(startServer ? "http" : "cli");

        // 2) Forces the app type, guarantees no server for CLI
        app.setWebApplicationType(startServer ? WebApplicationType.SERVLET : WebApplicationType.NONE);

        app.run(args);
    }

}
