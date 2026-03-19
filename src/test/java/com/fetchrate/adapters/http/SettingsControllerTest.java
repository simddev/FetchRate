package com.fetchrate.adapters.http;

import com.fetchrate.persistence.RateDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private RateDatabase database;

    @InjectMocks
    private SettingsController controller;

    // --- GET /settings ---

    @Test
    void getSettings_keyConfigured_returnsTrue() {
        when(database.getMeta("crypto_api_key")).thenReturn("some-key");
        when(database.getMeta("crypto_provider_url")).thenReturn(null);

        var result = controller.getSettings();

        assertEquals(true, result.get("apiKeyConfigured"));
        assertNull(result.get("providerUrl"));
    }

    @Test
    void getSettings_keyNotSet_returnsFalse() {
        when(database.getMeta("crypto_api_key")).thenReturn(null);
        when(database.getMeta("crypto_provider_url")).thenReturn(null);

        var result = controller.getSettings();

        assertEquals(false, result.get("apiKeyConfigured"));
        assertNull(result.get("providerUrl"));
    }

    @Test
    void getSettings_customUrlSet_returnsUrl() {
        when(database.getMeta("crypto_api_key")).thenReturn("some-key");
        when(database.getMeta("crypto_provider_url")).thenReturn("https://custom.example.com/api");

        var result = controller.getSettings();

        assertEquals("https://custom.example.com/api", result.get("providerUrl"));
    }

    // --- POST /settings ---

    @Test
    void saveSettings_validKey_savesAndReturns200() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("apiKey", "my-api-key"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("crypto_api_key", "my-api-key");
        verify(database, never()).setMeta(eq("crypto_provider_url"), any());
    }

    @Test
    void saveSettings_validUrl_savesAndReturns200() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("providerUrl", "https://custom.example.com/api"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("crypto_provider_url", "https://custom.example.com/api");
        verify(database, never()).setMeta(eq("crypto_api_key"), any());
    }

    @Test
    void saveSettings_bothFields_savesBoth() {
        ResponseEntity<?> response = controller.saveSettings(Map.of(
                "apiKey", "my-key",
                "providerUrl", "https://custom.example.com/api"
        ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("crypto_api_key", "my-key");
        verify(database).setMeta("crypto_provider_url", "https://custom.example.com/api");
    }

    @Test
    void saveSettings_emptyBody_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(database);
    }

    @Test
    void saveSettings_blankKeyOnly_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("apiKey", "  "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(database);
    }

    @Test
    void saveSettings_keyTrimmed_savedWithoutWhitespace() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("apiKey", "  my-key  "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("crypto_api_key", "my-key");
    }

    @Test
    void saveSettings_invalidUrl_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("providerUrl", "not a url at all"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(database, never()).setMeta(eq("crypto_provider_url"), any());
    }

    @Test
    void saveSettings_nonHttpScheme_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("providerUrl", "ftp://example.com/api"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(database, never()).setMeta(eq("crypto_provider_url"), any());
    }

    @Test
    void saveSettings_httpUrlAllowed() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("providerUrl", "http://localhost:8080/api"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("crypto_provider_url", "http://localhost:8080/api");
    }
}
