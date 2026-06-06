package com.fetchrate.adapters.http;

import com.fetchrate.core.Convertor;
import com.fetchrate.core.CurrencyClassifier;
import com.fetchrate.core.QueryRecord;
import com.fetchrate.core.RateNotFoundException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RateUpdater rateUpdater;
    @Mock
    private Convertor convertor;
    @Mock
    private CurrencyClassifier classifier;

    @InjectMocks
    private RequestController controller;

    @Test
    void convert_validRequest_returns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_futureDate_returns400() {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        ResponseEntity<?> response = controller.convert("100", "USD", tomorrow, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_negativeAmount_returns400() {
        ResponseEntity<?> response = controller.convert("-50", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_zeroAmount_returns400() {
        ResponseEntity<?> response = controller.convert("0", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_invalidDateFormat_returns400() {
        ResponseEntity<?> response = controller.convert("100", "USD", "15-01-2024", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_invalidAmountFormat_returns400() {
        ResponseEntity<?> response = controller.convert("abc", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_blankCurrency_returns400() {
        ResponseEntity<?> response = controller.convert("100", "  ", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_amountWithCommas_parsesAndReturns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91500.00"));

        ResponseEntity<?> response = controller.convert("100,000", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_unsupportedCurrency_returns400() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class)))
                .thenThrow(new IllegalArgumentException("Unsupported currency: FAKE"));

        ResponseEntity<?> response = controller.convert("100", "FAKE", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void convert_rateNotFound_returns404() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class)))
                .thenThrow(new RateNotFoundException("No rate found for USD on 2024-01-15"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void convert_unexpectedError_returns500() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class)))
                .thenThrow(new RuntimeException("Unexpected failure"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void convert_amountWithUnderscores_parsesAndReturns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91500.00"));

        ResponseEntity<?> response = controller.convert("100_000", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_lowercaseCurrency_isNormalized() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "usd", "2024-01-15", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_today_returns200() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        ResponseEntity<?> response = controller.convert("100", "USD", LocalDate.now().toString(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void convert_updatesRatesIfNotUpdatedToday() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(false);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("92.50"));

        controller.convert("100", "USD", "2024-01-15", null);

        verify(rateUpdater).updateRates();
    }

    @Test
    void convert_blankAmount_returns400() {
        ResponseEntity<?> response = controller.convert("  ", "USD", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_nullAmount_returns400() {
        ResponseEntity<?> response = controller.convert(null, "USD", "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_nullCurrency_returns400() {
        ResponseEntity<?> response = controller.convert("100", null, "2024-01-15", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_nullDate_returns400() {
        ResponseEntity<?> response = controller.convert("100", "USD", null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(convertor);
    }

    @Test
    void convert_outputCurrencyGbp_callsConvertToAndReturnsCrossFormat() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("GBP")).thenReturn(true);
        when(convertor.convertTo(any(QueryRecord.class), eq("GBP"))).thenReturn(new BigDecimal("78.65"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "GBP");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        var output = (Map<String, String>) body.get("output");
        assertEquals("78.65", output.get("amount"));
        assertEquals("GBP", output.get("currency"));
        assertNull(output.get("inEuro"));
        verify(convertor).convertTo(any(), eq("GBP"));
        verify(convertor, never()).convert(any());
    }

    @Test
    void convert_outputCurrencyEth_callsConvertToCrypto() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("ETH")).thenReturn(false);
        when(convertor.convertToCrypto(any(QueryRecord.class), eq("ETH"))).thenReturn(new BigDecimal("0.02500000"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "ETH");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        var output = (Map<String, String>) body.get("output");
        assertEquals("0.02500000", output.get("amount"));
        assertEquals("ETH", output.get("currency"));
        verify(convertor).convertToCrypto(any(), eq("ETH"));
        verify(convertor, never()).convert(any());
    }

    @Test
    void convert_outputCurrencyLowercase_isNormalized() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("GBP")).thenReturn(true);
        when(convertor.convertTo(any(QueryRecord.class), eq("GBP"))).thenReturn(new BigDecimal("78.65"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "gbp");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(convertor).convertTo(any(), eq("GBP"));
    }

    @Test
    void convert_outputCurrencyEur_usesDefaultEurPath() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91.37"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "EUR");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(convertor).convert(any());
        verify(convertor, never()).convertTo(any(), any());
    }

    @Test
    void convert_outputCurrencyUnknownSymbol_returns404() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(classifier.isSupportedOutputCurrency("FAKE")).thenReturn(false);
        when(convertor.convertToCrypto(any(QueryRecord.class), eq("FAKE")))
                .thenThrow(new RateNotFoundException("No crypto rate found for FAKE on 2024-01-15"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "FAKE");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void convert_blankOutputCurrency_treatedAsEur() {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any(QueryRecord.class))).thenReturn(new BigDecimal("91.37"));

        ResponseEntity<?> response = controller.convert("100", "USD", "2024-01-15", "   ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(convertor).convert(any());
        verify(convertor, never()).convertTo(any(), any());
        verify(convertor, never()).convertToCrypto(any(), any());
    }

    @Test
    void health_returnsOk() {
        var result = controller.health();
        assertEquals("ok", result.get("status"));
    }
}
