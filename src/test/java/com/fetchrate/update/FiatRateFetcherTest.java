package com.fetchrate.update;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FiatRateFetcherTest {

    private HttpServer server;
    private FiatRateFetcher fetcher;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        fetcher = new FiatRateFetcher();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchFiat_200_returnsBody() throws Exception {
        String xml = "<gesmes:Envelope>some xml</gesmes:Envelope>";
        server.createContext("/ecb", exchange -> {
            byte[] body = xml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        String result = fetcher.fetchFiat("http://localhost:" + port + "/ecb");

        assertEquals(xml, result);
    }

    @Test
    void fetchFiat_non200_throwsRuntimeException() throws Exception {
        server.createContext("/ecb", exchange -> {
            byte[] body = "Service Unavailable".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fetcher.fetchFiat("http://localhost:" + port + "/ecb"));

        assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    void fetchFiat_404_throwsRuntimeException() throws Exception {
        server.createContext("/ecb", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fetcher.fetchFiat("http://localhost:" + port + "/ecb"));

        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    void fetchFiat_connectionRefused_throwsRuntimeException() {
        // No server started on this port
        int deadPort = port + 1;

        assertThrows(RuntimeException.class, () ->
                fetcher.fetchFiat("http://localhost:" + deadPort + "/ecb"));
    }
}
