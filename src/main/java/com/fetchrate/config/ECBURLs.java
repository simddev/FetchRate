package com.fetchrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties holder for the three ECB exchange rate feed URLs.
 * Bound from the {@code ecb.fiat.*} prefix in {@code application-*.properties}.
 * <ul>
 *   <li>{@code dailyUrl} - today's rates only; used when the database is already up to date</li>
 *   <li>{@code days90Url} - last 90 days; used when the database is 2 - 89 days behind</li>
 *   <li>{@code fullUrl} - complete history; used on first run or after a gap of 90+ days</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "ecb.fiat")
public class ECBURLs {

    private String dailyURL;
    private String fullURL;
    private String days90URL;

    public String getDailyURL() {
        return dailyURL;
    }

    public void setDailyURL(String dailyURL) {
        this.dailyURL = dailyURL;
    }

    public String getFullURL() {
        return fullURL;
    }

    public void setFullURL(String fullURL) {
        this.fullURL = fullURL;
    }

    public String getDays90URL() {
        return days90URL;
    }

    public void setDays90URL(String days90URL) {
        this.days90URL = days90URL;
    }
}
