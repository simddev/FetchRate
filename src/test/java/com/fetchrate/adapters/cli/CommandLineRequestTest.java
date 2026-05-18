package com.fetchrate.adapters.cli;

import com.fetchrate.core.Convertor;
import com.fetchrate.update.CryptoRateUpdater;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandLineRequestTest {

    @Mock
    private RateUpdater rateUpdater;
    @Mock
    private Convertor convertor;
    @Mock
    private CryptoRateUpdater cryptoUpdater;

    private CommandLineRequest cli;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        cli = new CommandLineRequest(rateUpdater, convertor, new ObjectMapper(), cryptoUpdater);
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
    void run_helpFlag_outputsFullHelp() throws Exception {
        cli.run("--help");
        String out = output();
        assertTrue(out.contains("start_http_server"));
        assertTrue(out.contains("convert"));
        assertTrue(out.contains("config"));
        assertTrue(out.contains("--amount"));
    }

    @Test
    void run_shortHelpFlag_outputsFullHelp() throws Exception {
        cli.run("-h");
        assertTrue(output().contains("start_http_server"));
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
        when(convertor.convertTo(any(), any())).thenReturn(new BigDecimal("92.50"));

        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "2024-01-15");

        String out = output();
        assertTrue(out.contains("currencySymbol"));
        assertTrue(out.contains("\"currency\""));
        assertTrue(out.contains("EUR"));
        assertTrue(out.contains("92.50"));
    }

    @Test
    void run_shortFlags_outputsJson() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), any())).thenReturn(new BigDecimal("92.50"));

        cli.run("convert", "-a", "100", "-c", "USD", "-d", "2024-01-15");

        String out = output();
        assertTrue(out.contains("\"currency\""));
        assertTrue(out.contains("92.50"));
    }

    @Test
    void run_amountWithUnderscores_outputsJson() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), any())).thenReturn(new BigDecimal("91500.00"));

        cli.run("convert", "--amount", "100_000", "--input-currency", "USD", "--date", "2024-01-15");

        assertTrue(output().contains("\"currency\""));
    }

    @Test
    void run_lowercaseCurrency_isNormalized() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), any())).thenReturn(new BigDecimal("92.50"));

        cli.run("convert", "--amount", "100", "--input-currency", "usd", "--date", "2024-01-15");

        String out = output();
        assertTrue(out.contains("USD"));
    }

    @Test
    void run_configNoArgs_outputsUsage() throws Exception {
        cli.run("config");
        assertTrue(output().contains("Usage"));
    }

    @Test
    void run_configSetKey_writesFileAndOutputsSaved() throws Exception {
        java.nio.file.Path propsFile = java.nio.file.Path.of("fetchrate.properties");
        try {
            cli.run("config", "--set-key", "test-api-key-abc");
            assertTrue(output().contains("saved"));
            assertTrue(java.nio.file.Files.exists(propsFile));
            String content = java.nio.file.Files.readString(propsFile);
            assertTrue(content.contains("fetchrate.api-key=test-api-key-abc"));
        } finally {
            java.nio.file.Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void run_configSetUrl_writesFileAndOutputsSaved() throws Exception {
        java.nio.file.Path propsFile = java.nio.file.Path.of("fetchrate.properties");
        try {
            cli.run("config", "--set-url", "https://custom.example.com/api");
            assertTrue(output().contains("saved"));
            String content = java.nio.file.Files.readString(propsFile);
            assertTrue(content.contains("fetchrate.provider-url=https://custom.example.com/api"));
        } finally {
            java.nio.file.Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void run_configSetKey_handlesSpecialRegexCharsInValue() throws Exception {
        java.nio.file.Path propsFile = java.nio.file.Path.of("fetchrate.properties");
        try {
            // Keys with $ or \ would break replaceAll without Matcher.quoteReplacement
            cli.run("config", "--set-key", "key$with\\special");
            assertTrue(output().contains("saved"));
            String content = java.nio.file.Files.readString(propsFile);
            assertTrue(content.contains("fetchrate.api-key=key$with\\special"));
        } finally {
            java.nio.file.Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void run_configSetKey_updatesExistingEntry() throws Exception {
        java.nio.file.Path propsFile = java.nio.file.Path.of("fetchrate.properties");
        try {
            java.nio.file.Files.writeString(propsFile, "fetchrate.api-key=old-key\n");
            cli.run("config", "--set-key", "new-key");
            String content = java.nio.file.Files.readString(propsFile);
            assertTrue(content.contains("new-key"));
            assertFalse(content.contains("old-key"));
        } finally {
            java.nio.file.Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void run_configSetUrl_appendsToExistingFileWithoutDuplicatingContent() throws Exception {
        java.nio.file.Path propsFile = java.nio.file.Path.of("fetchrate.properties");
        try {
            java.nio.file.Files.writeString(propsFile, "fetchrate.api-key=my-key\n");
            cli.run("config", "--set-url", "https://custom.example.com/api");
            String content = java.nio.file.Files.readString(propsFile);
            assertTrue(content.contains("fetchrate.api-key=my-key"));
            assertTrue(content.contains("fetchrate.provider-url=https://custom.example.com/api"));
            // api-key line must appear exactly once (no duplication)
            assertEquals(1, content.lines().filter(l -> l.startsWith("fetchrate.api-key=")).count());
        } finally {
            java.nio.file.Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void run_currencyNotFound_outputsError() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), any())).thenThrow(new IllegalArgumentException("No rate found for FAKE"));

        cli.run("convert", "--amount", "100", "--input-currency", "FAKE", "--date", "2024-01-15");

        assertTrue(output().contains("error"));
        assertTrue(output().contains("FAKE"));
    }

    @Test
    void run_configAddSymbol_outputsAdded() throws Exception {
        cli.run("config", "--add-symbol", "XRP");

        assertTrue(output().contains("added"));
        assertTrue(output().contains("XRP"));
        verify(cryptoUpdater).addTrackedSymbol("XRP");
    }

    @Test
    void run_configAddSymbol_lowercaseIsNormalized() throws Exception {
        cli.run("config", "--add-symbol", "xrp");

        verify(cryptoUpdater).addTrackedSymbol("XRP");
    }

    @Test
    void run_configAddSymbol_invalidFormat_outputsError() throws Exception {
        cli.run("config", "--add-symbol", "invalid symbol!");

        assertTrue(output().contains("error"));
        verify(cryptoUpdater, never()).addTrackedSymbol(any());
    }

    @Test
    void run_configRemoveSymbol_outputsRemoved() throws Exception {
        cli.run("config", "--remove-symbol", "DOGE");

        assertTrue(output().contains("removed"));
        assertTrue(output().contains("DOGE"));
        verify(cryptoUpdater).removeTrackedSymbol("DOGE");
    }

    @Test
    void run_configRemoveSymbol_invalidFormat_outputsError() throws Exception {
        cli.run("config", "--remove-symbol", "invalid!");

        assertTrue(output().contains("error"));
        verify(cryptoUpdater, never()).removeTrackedSymbol(any());
    }

    @Test
    void run_configSetUrl_invalidScheme_outputsError() throws Exception {
        cli.run("config", "--set-url", "ftp://example.com");

        assertTrue(output().contains("error"));
        assertTrue(output().contains("http"));
    }

    @Test
    void run_configSetUrl_invalidFormat_outputsError() throws Exception {
        cli.run("config", "--set-url", "not a url");

        assertTrue(output().contains("error"));
    }

    @Test
    void run_configListSymbols_outputsJson() throws Exception {
        when(cryptoUpdater.getEffectiveSymbols()).thenReturn(java.util.List.of("BTC", "ETH", "XRP"));
        when(cryptoUpdater.isCustomized()).thenReturn(true);

        cli.run("config", "--list-symbols");

        String out = output();
        assertTrue(out.contains("symbols"));
        assertTrue(out.contains("BTC"));
        assertTrue(out.contains("XRP"));
        assertTrue(out.contains("\"customized\":true"));
    }

    @Test
    void run_toFlag_passesOutputCurrencyToConvertor() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), eq("GBP"))).thenReturn(new BigDecimal("78.40"));

        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "2024-01-15", "--to", "GBP");

        String out = output();
        assertTrue(out.contains("GBP"));
        assertTrue(out.contains("78.40"));
        verify(convertor).convertTo(any(), eq("GBP"));
    }

    @Test
    void run_shortToFlag_passesOutputCurrencyToConvertor() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), eq("JPY"))).thenReturn(new BigDecimal("13450.00"));

        cli.run("convert", "-a", "100", "-c", "USD", "-d", "2024-01-15", "-t", "JPY");

        verify(convertor).convertTo(any(), eq("JPY"));
    }

    @Test
    void run_toFlag_lowercaseIsNormalized() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), eq("GBP"))).thenReturn(new BigDecimal("78.40"));

        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "2024-01-15", "--to", "gbp");

        verify(convertor).convertTo(any(), eq("GBP"));
    }

    @Test
    void run_unsupportedOutputCurrency_outputsError() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertTo(any(), eq("FAKE")))
                .thenThrow(new IllegalArgumentException("Unsupported output currency: FAKE"));

        cli.run("convert", "--amount", "100", "--input-currency", "USD", "--date", "2024-01-15", "--to", "FAKE");

        String out = output();
        assertTrue(out.contains("error"));
        assertTrue(out.contains("FAKE"));
    }

    @Test
    void run_exchangeFlag_callsConvertToCrypto() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertToCrypto(any(), eq("ETH"))).thenReturn(new BigDecimal("12.34567890"));

        cli.run("convert", "--amount", "1", "--input-currency", "BTC", "--date", "2024-01-15", "--exchange", "ETH");

        String out = output();
        assertTrue(out.contains("ETH"));
        assertTrue(out.contains("12.34567890"));
        verify(convertor).convertToCrypto(any(), eq("ETH"));
        verify(convertor, never()).convertTo(any(), any());
    }

    @Test
    void run_shortExchangeFlag_callsConvertToCrypto() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertToCrypto(any(), eq("SOL"))).thenReturn(new BigDecimal("5.00000000"));

        cli.run("convert", "-a", "1", "-c", "BTC", "-d", "2024-01-15", "-e", "SOL");

        verify(convertor).convertToCrypto(any(), eq("SOL"));
    }

    @Test
    void run_exchangeFlag_lowercaseIsNormalized() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertToCrypto(any(), eq("ETH"))).thenReturn(new BigDecimal("12.00000000"));

        cli.run("convert", "-a", "1", "-c", "BTC", "-d", "2024-01-15", "--exchange", "eth");

        verify(convertor).convertToCrypto(any(), eq("ETH"));
    }

    @Test
    void run_bothToAndExchange_outputsError() throws Exception {
        cli.run("convert", "-a", "1", "-c", "BTC", "-d", "2024-01-15", "--to", "USD", "--exchange", "ETH");

        String out = output();
        assertTrue(out.contains("error"));
        assertTrue(out.contains("--to"));
        assertTrue(out.contains("--exchange"));
        verifyNoInteractions(convertor);
    }

    @Test
    void run_exchangeFlag_fiatSymbol_outputsError() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertToCrypto(any(), eq("USD")))
                .thenThrow(new IllegalArgumentException("USD is a fiat currency. Use --to USD for fiat output."));

        cli.run("convert", "-a", "100", "-c", "BTC", "-d", "2024-01-15", "--exchange", "USD");

        String out = output();
        assertTrue(out.contains("error"));
        assertTrue(out.contains("--to"));
    }

    @Test
    void run_exchangeFlag_cryptoNotFound_outputsError() throws Exception {
        when(rateUpdater.alreadyUpdatedToday()).thenReturn(true);
        when(convertor.convertToCrypto(any(), eq("XRP")))
                .thenThrow(new IllegalArgumentException("No crypto rate found for XRP"));

        cli.run("convert", "-a", "1", "-c", "BTC", "-d", "2024-01-15", "--exchange", "XRP");

        String out = output();
        assertTrue(out.contains("error"));
        assertTrue(out.contains("XRP"));
    }
}
