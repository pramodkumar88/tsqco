package com.tsqco.service.impl;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.SmartStreamSubsMode;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.helper.CacheHelper;
import com.tsqco.helper.DataProcessHelper;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.TsqcoAngelStockTransaction;
import com.tsqco.models.dto.*;
import com.tsqco.repo.TsqcoAngelInstrumentsRepo;
import com.tsqco.repo.TsqcoAngelStockTransactionRepo;
import com.tsqco.service.TsqcoAngelAppConfigService;
import com.tsqco.service.TsqcoComputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ProfitLossCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.supertrend.SuperTrendIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import reactor.util.retry.Retry;
import wiremock.Run;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import static com.tsqco.helper.NotificationFormatHelper.stockTransactionUpdate;
import static com.tsqco.helper.NotificationHelper.sendMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class TsqcoComputationServiceImpl implements TsqcoComputationService {

    private final TsqcoConfig tsqcoConfig;

    private final TsqcoAngelInstrumentsRepo tsqcoAngelInstrumentsRepo;

    private final TsqcoAngelStockTransactionRepo tsqcoAngelStockTransactionHistoryRepo;

    private final StringRedisTemplate redisTemplate;

    private final CacheHelper cacheHelper;

    private final TsqcoAngelAppConfigService tsqcoAngelAppConfigService;
    
    @Inject
    @Qualifier("Analysis")
    private final WebClient webClient;

    @Value("${google.spreadsheetid}")
    private String spreadSheetId;

    private final Map<String, List<AngelRecommendationDataDTO>> recommendationHistory = new ConcurrentHashMap<>();
    @Override
    public List<AngelCandleStickResponseDTO> getCandleStickData(AngelCandleStickRequestDTO angelCandleStickDataDTO) throws SmartAPIException, IOException {

        JSONObject requestObject = new JSONObject();
        requestObject.put("exchange", angelCandleStickDataDTO.getExchange());
        requestObject.put("symboltoken", angelCandleStickDataDTO.getToken());
        requestObject.put("interval", angelCandleStickDataDTO.getInterval());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        requestObject.put("fromdate", angelCandleStickDataDTO.getFromDate().format(formatter));
        requestObject.put("todate", angelCandleStickDataDTO.getToDate().format(formatter));
        JSONArray jsonArray = tsqcoConfig.getSmartConnect().candleData(requestObject);List<AngelCandleStickResponseDTO> dtoList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray data = jsonArray.getJSONArray(i);
            AngelCandleStickResponseDTO dto = new AngelCandleStickResponseDTO();
            dto.setDatetime(data.getString(0));
            dto.setOpen(data.getDouble(1));
            dto.setHigh(data.getDouble(2));
            dto.setLow(data.getDouble(3));
            dto.setClose(data.getDouble(4));
            dto.setVolume(data.getDouble(5));
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public List<TsqcoAngelInstruments> getSearchResults(String tradingSymbol, String exSegment) {
       return exSegment.equalsIgnoreCase("ALL")
                ? tsqcoAngelInstrumentsRepo.findDistinctBySymbolStartsWith(tradingSymbol)
                : tsqcoAngelInstrumentsRepo.findDistinctBySymbolStartsWithAndExchseg(tradingSymbol, exSegment);
    }

    @Override
    public void getBackTestResult(AngelBackTestDTO backTest) throws SmartAPIException, IOException {
        AngelCandleStickRequestDTO angelCandleStickDataDTO = new AngelCandleStickRequestDTO();
        angelCandleStickDataDTO.setExchange(backTest.getExchange());
        angelCandleStickDataDTO.setToken(backTest.getToken());
        angelCandleStickDataDTO.setInterval(backTest.getInterval());
        angelCandleStickDataDTO.setFromDate(backTest.getFromDate());
        angelCandleStickDataDTO.setToDate(backTest.getToDate());
        List<AngelCandleStickResponseDTO> candleStickData = getCandleStickData(angelCandleStickDataDTO);

        BarSeries series = new BaseBarSeriesBuilder().withName("mySeries").build();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        for (AngelCandleStickResponseDTO dto : candleStickData) {
            ZonedDateTime endTime = ZonedDateTime.parse(dto.getDatetime(), formatter);
            double open = dto.getOpen();
            double high = dto.getHigh();
            double low = dto.getLow();
            double close = dto.getClose();
            double volume = dto.getVolume();
            series.addBar(endTime, open, high, low, close, volume);
        }

        ///Need to implement strong strategies - Pending

        try {
            boolean isConnectionOpen = tsqcoConfig.smartStreamTicker(redisTemplate).isConnectionOpen();
            if(isConnectionOpen){
                Set<TokenID> tokenSet = new HashSet<>();
                tokenSet.add(new TokenID(ExchangeType.NSE_CM, "3456"));
                tokenSet.add(new TokenID(ExchangeType.NSE_CM, "20855"));
                tokenSet.add(new TokenID(ExchangeType.NSE_CM, "383"));
                tokenSet.add(new TokenID(ExchangeType.NSE_CM, "11377"));
                tsqcoConfig.smartStreamTicker(redisTemplate).subscribe(SmartStreamSubsMode.SNAP_QUOTE,tokenSet);
            }
        } catch (Exception ex ){
            log.error("Error Occurred"+ex);
        }


        int atrPeriod = 9;
        double multiplier = 3.0;

        // Create SuperTrendIndicator
        SuperTrendIndicator superTrendIndicator = new SuperTrendIndicator(series, atrPeriod, multiplier);

        // Lists to store the start indices of uptrends and downtrends
        List<Integer> uptrendStarts = new ArrayList<>();
        List<Integer> downtrendStarts = new ArrayList<>();

        List<String> uptrendTimes = new ArrayList<>();
        List<String> downtrendTimes = new ArrayList<>();

        List<Num> uptrendPrices = new ArrayList<>();
        List<Num> downtrendPrices = new ArrayList<>();

        // Initialize variables to track the current trend state
        boolean inUptrend = false;
        boolean inDowntrend = false;

        // Iterate through the series to detect trend changes
        for (int i = atrPeriod; i <= series.getEndIndex(); i++) {
            Num superTrendValue = superTrendIndicator.getValue(i);
            Bar bar = series.getBar(i);
            Num currentPrice = bar.getClosePrice();
            String time = bar.getEndTime().toString(); // Assuming getEndTime() returns a proper time format

            // Determine current trend based on price and SuperTrend value
            boolean isCurrentlyUptrend = currentPrice.isGreaterThan(superTrendValue);
            boolean isCurrentlyDowntrend = currentPrice.isLessThan(superTrendValue);

            // Check if there was a change in trend
            if (isCurrentlyUptrend && !inUptrend) {
                // Start of an uptrend
                uptrendStarts.add(i);
                uptrendTimes.add(time);
                uptrendPrices.add(currentPrice);
                inUptrend = true;
                inDowntrend = false;
            } else if (isCurrentlyDowntrend && !inDowntrend) {
                // Start of a downtrend
                downtrendStarts.add(i);
                downtrendTimes.add(time);
                downtrendPrices.add(currentPrice);
                inDowntrend = true;
                inUptrend = false;
            }
        }

        // Output the detected trend changes with time and price
        System.out.println("Uptrend starts:");
        for (int j = 0; j < uptrendStarts.size(); j++) {
            System.out.println("Index: " + uptrendStarts.get(j) + ", Time: " + uptrendTimes.get(j) + ", Price: " + uptrendPrices.get(j));
        }

        System.out.println("Downtrend starts:");
        for (int j = 0; j < downtrendStarts.size(); j++) {
            System.out.println("Index: " + downtrendStarts.get(j) + ", Time: " + downtrendTimes.get(j) + ", Price: " + downtrendPrices.get(j));
        }

        BarSeriesManager seriesManager = new BarSeriesManager(series);

        Strategy myStrategy = buildStrategy(series);

        TradingRecord tradingRecord = seriesManager.run(myStrategy);

        Num profit = new ReturnCriterion().calculate(series, tradingRecord);

        Num profitOrLoss = new ProfitLossCriterion().calculate(series, tradingRecord);

        System.out.println("Total profit: " + profit);

        System.out.println("Total profit: " + profitOrLoss);

    }

    @Override
    public void loadEpsData() throws RuntimeException {
       try {
           List<TsqcoInstrumentByEquityDTO> intrumentList = tsqcoAngelInstrumentsRepo.findInstrumentsBySymbolSuffix("-EQ");
           if (intrumentList.size() > 0) {
               fetchEPS(intrumentList);
           } else {
               throw new Exception("No Instrument Found!!");
           }
       } catch (Exception ex) {
           throw new RuntimeException(ex.getMessage());
       }
    }

    public static Strategy buildStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator longSma = new SMAIndicator(closePrice, 20);
        CrossedUpIndicatorRule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        CrossedDownIndicatorRule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);
        return new BaseStrategy(entryRule, exitRule);
    }


    public Sheets getSheetsService() throws RuntimeException {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("src/main/resources/static/credentials.json"))
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            return new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName("Spring Boot Google Sheets Integration").build();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }


    public void fetchEPS(List<TsqcoInstrumentByEquityDTO> symbols) throws RuntimeException {
        try {
            Sheets sheetsService = getSheetsService();
            // Determine the column and row of the start cell
            String column = "A"; // Fixed column for startCell as A1
            int startRow = 1; // Fixed row for startCell as A1

            // Prepare the formulas for all symbols
            List<List<Object>> formulas = new ArrayList<>();
            for (TsqcoInstrumentByEquityDTO symbol : symbols) {
                String formula = "=GOOGLEFINANCE(\"NSE:" + symbol.getName() + "\", \"eps\")";
                formulas.add(Collections.singletonList(formula));
            }

            // Define the range where formulas will be written
            String endCell = column + (startRow + symbols.size() - 1);
            ValueRange requestBody = new ValueRange().setValues(formulas);

            sheetsService.spreadsheets().values()
                    .update(spreadSheetId, column + startRow + ":" + endCell, requestBody)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            // Read the results in bulk
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadSheetId, column + startRow + ":" + endCell)
                    .execute();

            Map<String, Float> epsResults = new HashMap<>();
            List<List<Object>> values = response.getValues();
            for (int i = 0; i < symbols.size(); i++) {
                String symbol = symbols.get(i).getName();
                String epsValue = values.size() > i && values.get(i).size() > 0 ? values.get(i).get(0).toString() : "N/A";
                epsResults.put(symbol, epsValue.equals("#N/A") ? 0.0f : Float.parseFloat(epsValue));

            }
            updateEpsValues(epsResults);

            cleartheSheet(sheetsService, column + startRow + ":" + endCell);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private void cleartheSheet(Sheets sheetsService, String range) throws RuntimeException {
        try {
            // Clearing the Spread Sheet after successfully loading the data in the DB
            sheetsService.spreadsheets().values()
                    .clear(spreadSheetId, range, new ClearValuesRequest())
                    .execute();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to clear column: " + ex.getMessage());
        }
    }

    public void updateEpsValues(Map<String, Float> epsMap) {
        for (Map.Entry<String, Float> entry : epsMap.entrySet()) {
            String name = entry.getKey();
            Float eps = entry.getValue();
            tsqcoAngelInstrumentsRepo.updateEpsByName(name, eps);
        }
    }

    @Override
    public String analyzeBuyorHoldStock(Map<String, List<String>> stockData) throws RuntimeException{
        try {
            //log.info("Analyzing buy data:: ");
            StringBuilder stockDataFormatted = new StringBuilder();
            String stockToken = "";
            Float ltp = 0.0f;
            // Iterate through the stock data and format the details into the required prompt format
            for (Map.Entry<String, List<String>> entry : stockData.entrySet()) {
                stockToken = entry.getKey();
                List<String> stockDetails = entry.getValue();

                // Start building the prompt with stock token and details
                stockDataFormatted.append("Analyze stocktoken ").append(stockToken).append(": ");
                for (String detail : stockDetails) {
                    stockDataFormatted.append(" - ").append(detail);
                    if (detail.contains("Current LTP:")) {
                        // Split the string by the colon and trim whitespace
                        String[] parts = detail.split(":");
                        if (parts.length > 1) {
                            ltp = Float.parseFloat(parts[1].trim()); // Return the trimmed LTP value
                        }
                    }
                }
                stockDataFormatted.append(" ");
            }
            stockDataFormatted.append(" Provide recommendation as 'buy' or 'hold' only and include the confidence level (10 to 100) only. No explanation is needed. Return this data in JSON format.");

            log.info("Data format:: " + stockDataFormatted);

            String requestPayload = String.format("""
                    {
                      "model": "buyholdsellassistant",
                      "prompt": "%s",
                      "stream": false,
                      "temperature": 0,
                      "top_k": 1,
                      "top_p": 0.1,
                      "response_format": {
                        "recommendation": "string",
                        "confidencelevel": "integer"
                      }
                    }
                    """, stockDataFormatted);

            String finalStockToken = stockToken;
            String response = "";
            try {
                Float finalLtp = ltp;
                response = CompletableFuture.supplyAsync(() -> webClient.post()
                                .header("Content-Type", "application/json")
                                .bodyValue(requestPayload)
                                .retrieve()
                                .bodyToMono(String.class)
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                                .timeout(Duration.ofSeconds(300))
                                .block())
                        .thenApply(rawResponse -> {
                            try {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonResponse = objectMapper.readTree(rawResponse);
                                if (jsonResponse.has("response")) {
                                    String responseJson = jsonResponse.get("response").asText();
                                    responseJson = responseJson.replace("```json\n", "").replace("\n```", "");
                                    JsonNode innerJson = objectMapper.readTree(responseJson);
                                    String stocktoken = finalStockToken;
                                    String recommendation = innerJson.get("recommendation").asText();
                                    int confidenceLevel = innerJson.has("confidence_level") ? innerJson.get("confidence_level").asInt() :
                                            (innerJson.has("confidenceLevel") ? innerJson.get("confidenceLevel").asInt() :
                                                    innerJson.has("confidencelevel") ? innerJson.get("confidencelevel").asInt() : 0);

                                    return String.format("""
                                            {
                                              "stockToken": "%s",
                                              "recommendation": "%s",
                                              "confidenceLevel": %d,
                                              "ltp": %f
                                            }
                                            """, stocktoken, recommendation, confidenceLevel, finalLtp);
                                } else {
                                    log.error("{\"error\": \"Invalid response format\"}");
                                    return "{\"error\": \"Invalid response format\"}";
                                }
                            } catch (Exception e) {
                                log.error("Error parsing response: " + e.getMessage());
                                return "{\"error\": \"Error parsing response\"}";
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("Exception occurred: " + ex.getMessage());
                            return "{\"error\": \"Exception occurred\"}";
                        })
                        .join(); // Block here only for this method's completion
            } catch (Exception e) {
                log.error("Unexpected error occurred: " + e.getMessage());
                response = "{\"error\": \"Unexpected error occurred\"}";
            }
            log.info(response);
            return response;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public String analyzeSellorHoldStock(Map<String, List<String>> stockData) throws RuntimeException {
        try {
            log.info("Analyzing sell data:: ");
            StringBuilder stockDataFormatted = new StringBuilder();
            String stockToken = "";

            // Iterate through the stock data and format the details into the required prompt format
            for (Map.Entry<String, List<String>> entry : stockData.entrySet()) {
                stockToken = entry.getKey();
                List<String> stockDetails = entry.getValue();

                // Start building the prompt with stock token and details
                stockDataFormatted.append("Analyze stocktoken ").append(stockToken).append(": ");
                for (String detail : stockDetails) {
                    stockDataFormatted.append(" - ").append(detail);
                }
                stockDataFormatted.append(" ");
            }
            stockDataFormatted.append(" Provide recommendation as 'sell' or 'hold' only and include the confidence level (10 to 100) only. No explanation is needed. Return this data in JSON format.");

            log.info("Data format:: " + stockDataFormatted);

            String requestPayload = String.format("""
                    {
                      "model": "buyholdsellassistant",
                      "prompt": "%s",
                      "stream": false,
                      "temperature": 0,
                      "top_k": 1,
                      "top_p": 0.1,
                      "response_format": {
                        "recommendation": "string",
                        "confidencelevel": "integer"
                      }
                    }
                    """, stockDataFormatted);

            String finalStockToken = stockToken;
            String response = "";

            try {
                response = CompletableFuture.supplyAsync(() -> {
                            return webClient.post()
                                    .header("Content-Type", "application/json")
                                    .bodyValue(requestPayload)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                                    .timeout(Duration.ofSeconds(300))
                                    .block();
                        })
                        .thenApply(rawResponse -> {
                            //log.info("Response received:: " + rawResponse);

                            // Parse the response
                            try {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonResponse = objectMapper.readTree(rawResponse);
                                if (jsonResponse.has("response")) {
                                    String responseJson = jsonResponse.get("response").asText();
                                    responseJson = responseJson.replace("```json\n", "").replace("\n```", "");
                                    JsonNode innerJson = objectMapper.readTree(responseJson);
                                    String stocktoken = finalStockToken;
                                    String recommendation = innerJson.get("recommendation").asText();
                                    int confidenceLevel = innerJson.has("confidence_level") ? innerJson.get("confidence_level").asInt() :
                                            (innerJson.has("confidenceLevel") ? innerJson.get("confidenceLevel").asInt() :
                                                    innerJson.has("confidencelevel") ? innerJson.get("confidencelevel").asInt() : 0);

                                    return String.format("""
                                                {
                                                  "stocktoken": "%s",
                                                  "recommendation": "%s",
                                                  "confidenceLevel": %d
                                                }
                                            """, stocktoken, recommendation, confidenceLevel);
                                } else {
                                    log.error("{\"error\": \"Invalid response format\"}");
                                    return "{\"error\": \"Invalid response format\"}";
                                }
                            } catch (Exception e) {
                                log.error("Error parsing response: " + e.getMessage());
                                return "{\"error\": \"Error parsing response\"}";
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("Exception occurred: " + ex.getMessage());
                            return "{\"error\": \"Exception occurred\"}";
                        })
                        .join(); // Block here only for this method's completion
            } catch (Exception e) {
                log.error("Unexpected error occurred: " + e.getMessage());
                response = "{\"error\": \"Unexpected error occurred\"}";
            }
            log.info(response);
            return response;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public void handleRecommendation(AngelRecommendationDataDTO recommendationData) throws RuntimeException {
        try {
            String stockToken = recommendationData.getStockToken();
            String recommendation = recommendationData.getRecommendation();
            int confidenceLevel = recommendationData.getConfidenceLevel();

            // Initialize recommendation history for the token if absent
            recommendationHistory.putIfAbsent(stockToken, new ArrayList<>());

            // Add the current recommendation to the history
            recommendationHistory.get(stockToken).add(recommendationData);

            // Keep only the last two entries for the token
            if (recommendationHistory.get(stockToken).size() > 2) {
                recommendationHistory.get(stockToken).remove(0);
            }

            // Handle buy logic
            if ("buy".equalsIgnoreCase(recommendation) && confidenceLevel > 80) {
                long buyCount = recommendationHistory.get(stockToken).stream()
                        .filter(data -> "buy".equalsIgnoreCase(data.getRecommendation()) && data.getConfidenceLevel() > 80)
                        .count();
                if (buyCount >= 2 && !cacheHelper.isStockPurchased(stockToken.substring("stockData:".length()))) {
                    cacheHelper.storePurchasedStock(stockToken, 0.0); // Store stock with dummy purchase price
                    log.info("Buying stock: " + stockToken);
                    TsqcoAngelInstruments instrument = tsqcoAngelInstrumentsRepo.findByToken(stockToken.substring("stockData:NSE_CM-".length()));
                    TsqcoAngelStockTransaction buyTransaction = TsqcoAngelStockTransaction.builder()
                            .action("Buy")
                            .purchaseTime(LocalDateTime.now())
                            .token(instrument.getToken())
                            .name(instrument.getName())
                            .symbol(instrument.getSymbol())
                            .purchasePrice(recommendationData.getLtp())
                            .stockQuantity(1)
                            .build();
                    tsqcoAngelStockTransactionHistoryRepo.save(buyTransaction);
                    StringBuffer sb = new StringBuffer();
                    sb.append(instrument.getToken()).append(",")
                            .append(instrument.getSymbol()).append(",")
                            .append("Sell").append(",")
                            .append("1").append(",")
                            .append(recommendationData.getLtp()).append(",")
                            .append(LocalDateTime.now());
                    sendMessage(stockTransactionUpdate(sb.toString()));
                }
            }

            // Handle sell logic
            if ("sell".equalsIgnoreCase(recommendation)) {
                boolean shouldSell = recommendationHistory.get(stockToken).stream()
                        .anyMatch(data -> "sell".equalsIgnoreCase(data.getRecommendation()) && data.getConfidenceLevel() > 90)
                        || recommendationHistory.get(stockToken).stream()
                        .filter(data -> "sell".equalsIgnoreCase(data.getRecommendation()) && data.getConfidenceLevel() > 80)
                        .count() >= 2;
                if (shouldSell && cacheHelper.isStockPurchased(stockToken.substring("stockData:".length()))) {
                    log.info("Selling stock: " + stockToken);
                    TsqcoAngelInstruments instrument = tsqcoAngelInstrumentsRepo.findByToken(stockToken.substring("stockData:NSE_CM-".length()));
                    TsqcoAngelStockTransaction sellTransaction = TsqcoAngelStockTransaction.builder()
                            .action("Sell")
                            .sellTime(LocalDateTime.now())
                            .token(instrument.getToken())
                            .name(instrument.getName())
                            .symbol(instrument.getSymbol())
                            .sellPrice(recommendationData.getLtp())
                            .stockQuantity(1)
                            .build();
                    tsqcoAngelStockTransactionHistoryRepo.save(sellTransaction);
                    StringBuffer sb = new StringBuffer();
                    sb.append(instrument.getToken()).append(",")
                            .append(instrument.getSymbol()).append(",")
                            .append("Sell").append(",")
                            .append("1").append(",")
                            .append(recommendationData.getLtp()).append(",")
                            .append(LocalDateTime.now());
                    sendMessage(stockTransactionUpdate(sb.toString()));

                    cacheHelper.removePurchasedStock(stockToken);
                    cacheHelper.removeTokenFromCache(stockToken);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public void analyzeStockData(String token) throws RuntimeException {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            List<Callable<Void>> tasks = new ArrayList<>();
            tasks.add(() -> {
                boolean isPurchased = cacheHelper.isStockPurchased(token.substring("stockData:".length()));
                String purchasePrice = "";
                if (isPurchased) {
                    Map<Object, Object> purchaseData = cacheHelper.getPurchasedStockData(token.substring("stockData:".length()));
                    purchasePrice = (String) purchaseData.get("purchasePrice");
                }
                String[] stockData = cacheHelper.getStockData(token);
                DataProcessHelper processHelper = new DataProcessHelper(cacheHelper, tsqcoAngelAppConfigService);
                Map<String, List<String>> stringListMap = processHelper.processInputData(stockData, purchasePrice);
                AngelRecommendationDataDTO recommendationData = new AngelRecommendationDataDTO();
                recommendationData.setLtp(Float.parseFloat(stockData[0].split(" ")[5]));
                if (isPurchased) {
                    //log.info("Sell Analysis");
                    recommendationData = tsqcoConfig.mapJsonToObject(analyzeSellorHoldStock(stringListMap), AngelRecommendationDataDTO.class);
                    handleRecommendation(recommendationData);
                } else {
                    //log.info("Buy Analysis");
                    recommendationData = tsqcoConfig.mapJsonToObject(analyzeBuyorHoldStock(stringListMap), AngelRecommendationDataDTO.class);
                    handleRecommendation(recommendationData);
                }
                return null;
            });
            try {
                executorService.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Shut down the executor
                executorService.shutdown();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
