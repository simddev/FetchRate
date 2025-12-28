package com.fetchrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This class serves to provide getter methods which return the URLs for the Fiat data.
 * <p>
 * They can be found and adjusted in com.fetchrate/resources/application.properties.
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
