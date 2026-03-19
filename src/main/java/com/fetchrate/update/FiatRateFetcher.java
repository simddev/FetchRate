package com.fetchrate.update;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches raw ECB exchange rate data from a remote URL.
 * Used by {@link FiatRateUpdater} to retrieve the XML feed before parsing.
 */
@Service
public class FiatRateFetcher {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches the XML content at the given URL and returns it as a string.
     *
     * @param url The URL to fetch.
     * @return The response body as a string.
     * @throws RuntimeException if the server returns a non-200 status, the connection fails,
     *                          or the thread is interrupted.
     */
    public String fetchFiat(String url) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("ECB returned HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch fiat rates from ECB", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching fiat rates from ECB", e);
        }
    }
}
