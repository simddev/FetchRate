package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses cryptocurrency rate data from two formats into {@link com.fetchrate.core.CryptoRateRecord} lists:
 * CSV files (for historical bulk imports) and crypto data provider JSON API responses.
 * Malformed lines and entries are skipped individually so that one bad record does not
 * abort the entire parse.
 */
@Service
public class CryptoRateParser {

    private static final Logger log = LoggerFactory.getLogger(CryptoRateParser.class);

    private final ObjectMapper objectMapper;

    public CryptoRateParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a CSV file of historical cryptocurrency prices into a list of records.
     * <p>
     * The CSV must contain at least an {@code end} column (ISO date) and a {@code close} column
     * (closing price in EUR). Header names are matched case-insensitively. Lines with missing,
     * blank, or unparseable values are silently skipped.
     *
     * @param symbol The coin symbol (e.g., {@code BTC}); stored in upper case.
     * @param csv    The raw CSV content including a header row.
     * @return List of parsed {@link com.fetchrate.core.CryptoRateRecord} objects.
     */
    public List<CryptoRateRecord> parseCrypto(String symbol, String csv) {

        List<CryptoRateRecord> cryptoRecord = new ArrayList<>();
        String[] lines = csv.split("\\R");
        if (lines.length <= 1) return cryptoRecord;

        String[] header = lines[0].split(",", -1);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i].trim().toLowerCase(), i);
        }

        int startIndex = index.getOrDefault("end", 1);
        int closeIndex = index.getOrDefault("close", 5);

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] fields = line.split(",", -1);
            if (fields.length <= Math.max(startIndex, closeIndex)) continue;

            String dateString = fields[startIndex].trim();
            String closeString = fields[closeIndex].trim();

            if (dateString.isEmpty() || closeString.isEmpty()) continue;
            if ("null".equalsIgnoreCase(closeString)) continue;

            try {
                LocalDate date = LocalDate.parse(dateString);
                BigDecimal rate = new BigDecimal(closeString);
                cryptoRecord.add(new CryptoRateRecord(symbol.toUpperCase(), date, rate));
            } catch (Exception e) {
                log.debug("Skipping malformed CSV line for {}: {}", symbol, e.getMessage());
            }
        }

        return cryptoRecord;
    }

    /**
     * Parses a crypto data provider API JSON response into a list of records.
     * <p>
     * Attempts standard Jackson deserialization first; if that fails (e.g., truncated response),
     * falls back to regex-based manual extraction. The coin symbol is read from the {@code code}
     * field in the JSON when present, otherwise the provided {@code symbol} parameter is used.
     *
     * @param symbol Fallback coin symbol if none is found in the JSON.
     * @param json   The raw JSON string from the crypto data provider API.
     * @return List of parsed {@link com.fetchrate.core.CryptoRateRecord} objects.
     */
    public List<CryptoRateRecord> parseProviderResponse(String symbol, String json) {
        List<CryptoRateRecord> cryptoRecord = new ArrayList<>();
        try {
            if (json == null || json.isBlank()) return cryptoRecord;
            
            // Try regular Jackson parsing first
            try {
                JsonNode root = objectMapper.readTree(json);
                String symbolFromJson = root.path("code").asText().toUpperCase();
                String effectiveSymbol = symbolFromJson.isEmpty() ? symbol : symbolFromJson;
                
                JsonNode historyNode = root.path("history");
                if (historyNode.isArray()) {
                    for (JsonNode entry : historyNode) {
                        JsonNode dateNode = entry.path("date");
                        JsonNode rateNode = entry.path("rate");
                        if (!dateNode.isMissingNode() && !rateNode.isMissingNode()) {
                            long timestamp = dateNode.asLong();
                            BigDecimal rate = new BigDecimal(rateNode.asText());
                            // API returns history entries for a specific time. 
                            // We use UTC to ensure consistent date mapping.
                            LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate();
                            cryptoRecord.add(new CryptoRateRecord(effectiveSymbol, date, rate));
                        }
                    }
                }
            } catch (Exception e) {
                // Fall through to manual extraction
            }

            // Manual extraction for truncated or slightly malformed JSON
            if (cryptoRecord.isEmpty()) {
                manualExtractFromTruncatedJson(symbol, cryptoRecord, json);
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse crypto provider JSON for {}: {}", symbol, e.getMessage());
        }
        return cryptoRecord;
    }

    private void manualExtractFromTruncatedJson(String symbol, List<CryptoRateRecord> cryptoRecord, String json) {
        try {
            // Find symbol if present in JSON
            String symbolFromJson = null;
            java.util.regex.Pattern symbolPattern = java.util.regex.Pattern.compile("\"code\"\\s*:\\s*\"([A-Z0-9]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher symbolMatcher = symbolPattern.matcher(json);
            if (symbolMatcher.find()) {
                symbolFromJson = symbolMatcher.group(1).toUpperCase();
            }
            
            String effectiveSymbol = (symbolFromJson != null) ? symbolFromJson : symbol;

            java.util.regex.Pattern entryPattern = java.util.regex.Pattern.compile("\"date\"\\s*:\\s*(\\d+)\\s*,\\s*\"rate\"\\s*:\\s*([\\d.]+)");
            java.util.regex.Matcher entryMatcher = entryPattern.matcher(json);
            while (entryMatcher.find()) {
                try {
                    long timestamp = Long.parseLong(entryMatcher.group(1));
                    BigDecimal rate = new BigDecimal(entryMatcher.group(2));
                    LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate();
                    if (effectiveSymbol != null) {
                        cryptoRecord.add(new CryptoRateRecord(effectiveSymbol, date, rate));
                    }
                } catch (Exception e) {
                    log.debug("Skipping malformed entry during manual JSON extraction for {}: {}", symbol, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Manual JSON extraction failed for {}: {}", symbol, e.getMessage());
        }
    }
}
