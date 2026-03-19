package com.fetchrate.update;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * This class serves to provide the fetchFiat method, which fetches the raw .xml file from the provided URL,
 * and returns it in String form.
 * It is set up to be a bean serving the FiatRateUpdater class.
 */
@Service
public class FiatRateFetcher {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Method which takes a URL, fetches it via java.net.http packages, and returns it in String format.
     *
     * @param URL String of the URL.
     * @return String of the contents.
     */
    public String fetchFiat(String URL) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("ECB returned HTTP " + response.statusCode() + " for " + URL);
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
