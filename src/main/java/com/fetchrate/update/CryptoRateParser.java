package com.fetchrate.update;

import com.fetchrate.core.CryptoRateRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * This class serves to turn the data given into the proper format and create a
 * List out of it, which can be then stored in the database.
 */
@Service
public class CryptoRateParser {

    /**
     * This method parses the data given to it into a ready for database List.
     *
     * @param symbol The symbol of the cryptocurrency.
     * @param csv The raw data to be parsed.
     * @return Returns a List of CryptoRateRecord.
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

            String[] colons = line.split(",", -1);
            if (colons.length <= Math.max(startIndex, closeIndex)) continue;

            String dateString = colons[startIndex].trim();
            String closeString = colons[closeIndex].trim();

            if (dateString.isEmpty() || closeString.isEmpty()) continue;
            if ("null".equalsIgnoreCase(closeString)) continue;

            LocalDate date = LocalDate.parse(dateString);
            BigDecimal rate = new BigDecimal(closeString);

            cryptoRecord.add(new CryptoRateRecord(symbol.toUpperCase(), date, rate));
        }

        return cryptoRecord;
    }
}
