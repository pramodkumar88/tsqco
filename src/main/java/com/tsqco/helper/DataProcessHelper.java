package com.tsqco.helper;

import com.tsqco.service.TsqcoAngelAppConfigService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class DataProcessHelper {

    private final CacheHelper cacheHelper;

    private final TsqcoAngelAppConfigService tsqcoAngelAppConfigService;

    private static final String REDIS_KEY_PREFIX = "token:";

    public Map<String, List<String>> processInputData(String data[], String purchasePrice) {
        Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
        String subscriptionMode = tsqcoAngelAppConfigService.getConfigValue("subscriptionmode");
        LocalTime startTime = LocalTime.parse(tsqcoAngelAppConfigService.getConfigValue("markettimeopen")); // 9:00 AM
        LocalTime endTime = LocalTime.parse(tsqcoAngelAppConfigService.getConfigValue("markettimeclose"));

        for (String record : data) {
            String[] parts = record.split(" ");
            subscriptionMode = parts[1];
            String token = parts[3];
            float ltp = Float.parseFloat(parts[5]);
            float open = Float.parseFloat(parts[7]);
            float high = Float.parseFloat(parts[9]);
            float low = Float.parseFloat(parts[11]);
            float close = Float.parseFloat(parts[13]);

            // Extract and parse exchange time
            String exchangeTimePart = parts[15];
            exchangeTimePart = exchangeTimePart.substring(0, exchangeTimePart.indexOf('['));
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(exchangeTimePart, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            ZonedDateTime exchangeTime = offsetDateTime.atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata"));

            LocalTime recordTime = exchangeTime.toLocalTime();
            /*if (recordTime.isBefore(startTime) || recordTime.isAfter(endTime)) {
                continue; // Skip records outside the working hours
            }*/


            Map<String, Object> parsedData = new HashMap<>();

            parsedData.put("ltp", ltp);
            parsedData.put("open", open);
            parsedData.put("high", high);
            parsedData.put("low", low);
            parsedData.put("close", close);
            parsedData.put("exchangeTime", exchangeTime);

            if ("SNAP_QUOTE".equals(subscriptionMode)) {
                // Add additional data for SNAP_QUOTE subscription mode
                double upperCircuit = Math.round(Float.parseFloat(parts[17]) * 100.0) / 100.0;  // Round to 2 decimal places
                double lowerCircuit = Math.round(Float.parseFloat(parts[19]) * 100.0) / 100.0;  // Round to 2 decimal places
                double yearlyHighPrice = Math.round(Float.parseFloat(parts[21]) * 100.0) / 100.0;  // Round to 2 decimal places
                double yearlyLowPrice = Math.round(Float.parseFloat(parts[23]) * 100.0) / 100.0;  // Round to 2 decimal places
                double lastTradedQty = Math.round(Double.parseDouble(parts[25]) * 100.0) / 100.0;  // Round to 2 decimal places
                double avgTradedPrice = Math.round(Float.parseFloat(parts[27]) * 100.0) / 100.0;  // Round to 2 decimal places
                double volumeTradedToday = Math.round(Double.parseDouble(parts[29]) * 100.0) / 100.0;  // Round to 2 decimal places
                float totalBuyQty = Math.round(Float.parseFloat(parts[31]) * 100.0) / 100.0f;  // Round to 2 decimal places
                float totalSellQty = Math.round(Float.parseFloat(parts[33]) * 100.0) / 100.0f;  // Round to 2 decimal places

                // Add the rounded values to parsedData
                parsedData.put("upperCircuit", upperCircuit);
                parsedData.put("lowerCircuit", lowerCircuit);
                parsedData.put("yearlyHighPrice", yearlyHighPrice);
                parsedData.put("yearlyLowPrice", yearlyLowPrice);
                parsedData.put("lastTradedQty", lastTradedQty);
                parsedData.put("avgTradedPrice", avgTradedPrice);
                parsedData.put("volumeTradedToday", volumeTradedToday);
                parsedData.put("totalBuyQty", totalBuyQty);
                parsedData.put("totalSellQty", totalSellQty);
            }

            groupedData.computeIfAbsent(token, k -> new ArrayList<>()).add(parsedData);
        }


        // Aggregate data
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            String token = entry.getKey();
            List<Map<String, Object>> records = entry.getValue();

            // Sort records by exchange time
            records.sort(Comparator.comparing(r -> (ZonedDateTime) r.get("exchangeTime")));

            // Get oldest and newest times
            ZonedDateTime oldestTime = (ZonedDateTime) records.get(0).get("exchangeTime");
            ZonedDateTime newestTime = (ZonedDateTime) records.get(records.size() - 1).get("exchangeTime");
            long dataTimeSpanMinutes = java.time.Duration.between(oldestTime, newestTime).toMinutes();

            // Summaries
            List<String> summaries = new ArrayList<>();
            if(!purchasePrice.isEmpty()) {
                summaries.add("Purchased Price: "+purchasePrice);
                Float percentChange = getPercentageChange(Float.parseFloat(records.get(records.size() - 1).get("ltp").toString()), Float.parseFloat(purchasePrice));
                if( percentChange> 0){
                    summaries.add("Percentage Gain: "+percentChange);
                } else{
                    summaries.add("Percentage Loss: "+percentChange);
                }
            } else {
                String oldLTPStr = cacheHelper.getTokenData(REDIS_KEY_PREFIX + token.replace("NSE_CM-", ""));
                Float percentChange = getPercentageChange( Float.parseFloat(records.get(records.size() - 1).get("ltp").toString()), Float.parseFloat(oldLTPStr));
                if( percentChange> 0){
                    summaries.add("Percentage Gain: "+percentChange);
                } else{
                    summaries.add("Percentage Loss: "+percentChange);
                }
            }
            summaries.add("Current LTP: " + records.get(records.size() - 1).get("ltp"));
            summaries.add("Current Open: " + records.get(records.size() - 1).get("open"));
            summaries.add("Current High: " + records.get(records.size() - 1).get("high"));
            summaries.add("Current Low: " + records.get(records.size() - 1).get("low"));
            summaries.add("Current Close: " + records.get(records.size() - 1).get("close"));

            // Add minutes to market close
            ZonedDateTime latestExchangeTime = (ZonedDateTime) records.get(records.size() - 1).get("exchangeTime");
            LocalTime recordTime = latestExchangeTime.toLocalTime();
            long minutesToMarketClose = java.time.Duration.between(recordTime, endTime).toMinutes();
            if (minutesToMarketClose > 0) {
                summaries.add("Minutes to Market Close: " + minutesToMarketClose);
            } else {
                summaries.add("Market Closed");
            }


            // Add SNAP_QUOTE data if in SNAP_QUOTE mode
            if ("SNAP_QUOTE".equals(subscriptionMode)) {
                summaries.add("Current Upper Circuit: " + records.get(records.size() - 1).get("upperCircuit"));
                summaries.add("Current Lower Circuit: " + records.get(records.size() - 1).get("lowerCircuit"));
                summaries.add("Yearly High Price: " + records.get(records.size() - 1).get("yearlyHighPrice"));
                summaries.add("Yearly Low Price: " + records.get(records.size() - 1).get("yearlyLowPrice"));
            }

            // Aggregated ranges only within the actual data time span
            int[] timeRanges = {3, 5, 10, 20, 30, 40, 50, 60, 120}; // Minutes
            for (int minutes : timeRanges) {
                if (minutes <= dataTimeSpanMinutes) {
                    addRangeSummaries(records, "ltp", oldestTime, minutes, summaries);
                    addRangeSummaries(records, "open", oldestTime, minutes, summaries);
                    addRangeSummaries(records, "high", oldestTime, minutes, summaries);
                    addRangeSummaries(records, "low", oldestTime, minutes, summaries);
                    addRangeSummaries(records, "close", oldestTime, minutes, summaries);
                }
            }

            result.put(token, summaries);
        }
        return result;
    }

    private String determineSubscriptionMode(String[] data) {
        // Check if the data contains key attributes to decide the subscription mode
        for (String record : data) {
            String[] parts = record.split(" ");
            if (parts.length > 15) {
                // If Open Interest, Upper Circuit, Lower Circuit are present, it's SNAP_QUOTE
                if (parts[15] != null && !parts[15].isEmpty()) {
                    return "SNAP_QUOTE";
                }
            }
        }
        return "QUOTE"; // Default to QUOTE mode if no indicators of SNAP_QUOTE
    }

    private Float getPercentageChange(Float newLTP, Float oldLTP) {
       return ((newLTP - oldLTP) / oldLTP) * 100;
    }

    private void addRangeSummaries(List<Map<String, Object>> records, String field, ZonedDateTime referenceTime, int minutes, List<String> summaries) {
        List<Float> values = records.stream()
                .filter(r -> ((ZonedDateTime) r.get("exchangeTime")).isAfter(referenceTime.minusMinutes(minutes)))
                .map(r -> (Float) r.get(field))
                .collect(Collectors.toList());

        if (!values.isEmpty()) {
            double min = values.stream().min(Float::compare).orElse(Float.NaN);
            double max = values.stream().max(Float::compare).orElse(Float.NaN);
            min = Math.round(min * 100.0) / 100.0;
            max = Math.round(max * 100.0) / 100.0;
            summaries.add(field.toUpperCase() + " range in last " + minutes + " minutes: " + min + "-" + max);
        }
    }
}
