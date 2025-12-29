package com.fetchrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.api")
public record CryptoAPI(
        String baseUrl,
        String key,
        String keyHeader,
        String historicalPath,
        String quoteCurrency
) {}
