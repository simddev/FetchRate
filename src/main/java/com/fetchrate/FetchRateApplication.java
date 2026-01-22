package com.fetchrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * Checks if a local data folder exists, if not, creates it
     */
    private static void bootstrapDataFiles() {
        try {
            Path dataDir = Path.of("data");
            Files.createDirectories(dataDir);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize data folder", e);
        }
    }

    public static void main(String[] args) {

        bootstrapDataFiles();

        boolean startServer = args.length > 0 && "start_http_server".equals(args[0]);

        var context = new SpringApplication(FetchRateApplication.class);

        // 1) Picks a profile, application-cli.properties or application-http.properties
        context.setAdditionalProfiles(startServer ? "http" : "cli");

        // 2) Forces the app type, guarantees no server for CLI
        context.setWebApplicationType(startServer ? WebApplicationType.SERVLET : WebApplicationType.NONE);

        var app = context.run(args);
    }

}
