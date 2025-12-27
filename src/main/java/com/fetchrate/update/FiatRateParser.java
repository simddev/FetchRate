package com.fetchrate.update;

import com.fetchrate.core.ExchangeRateRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class serves to parse the data from a String format into a List of Records format.
 */
@Service
public class FiatRateParser {

    private static final Pattern DAY_BLOCK = Pattern.compile(
            "<Cube\\s+time=['\"](\\d{4}-\\d{2}-\\d{2})['\"]\\s*>\\s*(.*?)\\s*</Cube>",
            Pattern.DOTALL
    );


    private static final Pattern RATE_ROW = Pattern.compile(
            "<Cube\\s+currency=['\"]([A-Z]{3})['\"]\\s+rate=['\"]([0-9.]+)['\"]\\s*/>"
    );



    /**
     * This method takes a String of the data and converts it into a List of ExchangeRateRecord.
     * <p>
     * It uses regex to extract the data, since the .xml file is in a predictable format.
     *
     * @param xml String of the data to be parsed
     * @return ArrayList of ExchangeRateRecord
     */
    public List<ExchangeRateRecord> parseFiat(String xml) {

        List<ExchangeRateRecord> fiatRecord = new ArrayList<>();

        Matcher dayMatcher = DAY_BLOCK.matcher(xml);
        while (dayMatcher.find()) {
            LocalDate date = LocalDate.parse(dayMatcher.group(1));
            String inner = dayMatcher.group(2);

            Matcher rowMatcher = RATE_ROW.matcher(inner);
            while (rowMatcher.find()) {
                String currency = rowMatcher.group(1);
                BigDecimal rate = new BigDecimal(rowMatcher.group(2));

                fiatRecord.add(new ExchangeRateRecord(currency, date, rate));
            }
        }
        return fiatRecord;
    }
}