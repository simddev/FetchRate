package com.fetchrate.adapters.http;

import com.fetchrate.core.Convertor;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.update.RateUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RateUpdater rateUpdater;
    @Mock
    private Convertor convertor;

    @InjectMocks
    private RequestController controller;

    @Test
    void convert_validRequest_returns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_futureDate_returns400() {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        ResponseEntity<?> response = controller.convert("100", "USD", tomorrow);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_negativeAmount_returns400() {
        ResponseEntity<?> response = controller.convert("-50", "USD", "2024-01-15");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_zeroAmount_returns400() {
        ResponseEntity<?> response = controller.convert("0", "USD", "2024-01-15");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_invalidDateFormat_returns400() {
        ResponseEntity<?> response = controller.convert("100", "USD", "15-01-2024");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_invalidAmountFormat_returns400() {
        ResponseEntity<?> response = controller.convert("abc", "USD", "2024-01-15");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_blankCurrency_returns400() {
        ResponseEntity<?> response = controller.convert("100", "  ", "2024-01-15");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_amountWithCommas_parsesAndReturns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91500.00"));

        ResponseEntity<?> response = controller.convert("100,000", "USD", "2024-01-15");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_unsupportedCurrency_returns404() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class)))
                .thenThrow(new IllegalArgumentException("Unsupported currency: FAKE"));

        ResponseEntity<?> response = controller.convert("100", "FAKE", "2024-01-15");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void convert_unexpectedError_returns500() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class)))
                .thenThrow(new RuntimeException("Unexpected failure"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void convert_amountWithUnderscores_parsesAndReturns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91500.00"));

        ResponseEntity<?> response = controller.convert("100_000", "USD", "2024-01-15");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_lowercaseCurrency_isNormalized() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "usd", "2024-01-15");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_today_returns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "USD", LocalDate.now().toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_updatesRatesIfNotUpdatedToday() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(false);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        controller.convert("100", "USD", "2024-01-15");

        verify(rateUpdater).updateRates();
    }

    @Test
    void convert_nullCurrency_returns400() {
        ResponseEntity<?> response = controller.convert("100", null, "2024-01-15");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void health_returnsOk() {
        var result = controller.health();
        assertEquals("ok", result.get("status"));
    }
}
