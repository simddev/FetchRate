package com.fetchrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Application entry point. Selects the Spring profile and web type based on the first argument:
 * {@code start_http_server} activates the {@code http} profile with an embedded servlet container;
 * all other invocations activate the {@code cli} profile with no web server for fast startup.
 */
@SpringBootApplication
public class FetchRateApplication {

    /** Creates the {@code data/} directory if it does not already exist. */
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

        // Translate --port N → --server.port=N so Spring picks it up
        String[] springArgs = args;
        if (startServer) {
            List<String> argList = new ArrayList<>(Arrays.asList(args));
            for (int i = 1; i < argList.size() - 1; i++) {
                if ("--port".equals(argList.get(i))) {
                    String port = argList.remove(i + 1);
                    argList.set(i, "--server.port=" + port);
                    break;
                }
            }
            springArgs = argList.toArray(new String[0]);
        }

        var context = new SpringApplication(FetchRateApplication.class);

        // 1) Picks a profile, application-cli.properties or application-http.properties
        context.setAdditionalProfiles(startServer ? "http" : "cli");

        // 2) Forces the app type, guarantees no server for CLI
        context.setWebApplicationType(startServer ? WebApplicationType.SERVLET : WebApplicationType.NONE);

        context.run(springArgs);
    }

}
