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

    @Test
    void getSettings_keyConfigured_returnsTrue() {
        when(database.getMeta("livecoinwatch_api_key")).thenReturn("some-key");

        var result = controller.getSettings();

        assertEquals(true, result.get("apiKeyConfigured"));
    }

    @Test
    void getSettings_keyNotSet_returnsFalse() {
        when(database.getMeta("livecoinwatch_api_key")).thenReturn(null);

        var result = controller.getSettings();

        assertEquals(false, result.get("apiKeyConfigured"));
    }

    @Test
    void saveSettings_validKey_savesAndReturns200() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("apiKey", "my-api-key"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(database).setMeta("livecoinwatch_api_key", "my-api-key");
    }

    @Test
    void saveSettings_emptyKey_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of("apiKey", "  "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(database);
    }

    @Test
    void saveSettings_missingKey_returns400() {
        ResponseEntity<?> response = controller.saveSettings(Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(database);
    }
}
