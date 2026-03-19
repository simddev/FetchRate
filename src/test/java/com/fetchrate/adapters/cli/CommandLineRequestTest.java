package com.fetchrate.adapters.cli;

import com.fetchrate.core.Convertor;
import com.fetchrate.update.RateUpdater;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandLineRequestTest {

    @Mock
    private RateUpdater rateUpdater;
    @Mock
    private Convertor convertor;

    private CommandLineRequest cli;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        cli = new CommandLineRequest(rateUpdater, convertor, new ObjectMapper());
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String output() {
        return outContent.toString().trim();
    }

    @Test
    void run_noArgs_outputsUsage() throws Exception {
        cli.run();
        assertTrue(output().contains("Usage"));
    }

    @Test
    void run_unknownCommand_outputsUsage() throws Exception {
        cli.run("unknown");
        assertTrue(output().contains("Usage"));
    }

    @Test
    void run_futureDate_outputsError() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", tomorrow);

        assertTrue(output().contains("error"));
        assertTrue(output().contains("future"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_negativeAmount_outputsError() throws Exception {
        cli.run("convert", "--amount", "-100", "--input-currency", "USD", "--date", "2024-01-15");

        assertTrue(output().contains("error"));
        assertTrue(output().contains("greater than zero"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_zeroAmount_outputsError() throws Exception {
        cli.run("convert", "--amount", "0", "--input-currency", "USD", "--date", "2024-01-15");

        assertTrue(output().contains("error"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_invalidDateFormat_outputsError() throws Exception {
        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "15-01-2024");

        assertTrue(output().contains("error"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_invalidAmountFormat_outputsError() throws Exception {
        cli.run("convert", "--amount", "abc", "--input-currency", "USD", "--date", "2024-01-15");

        assertTrue(output().contains("error"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_validRequest_outputsJson() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convert(any())).thenReturn(new BigDecimal("92.50"));

        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "2024-01-15");

        String out = output();
        assertTrue(out.contains("currencySymbol"));
        assertTrue(out.contains("inEuro"));
        assertTrue(out.contains("92.50"));
    }
}
