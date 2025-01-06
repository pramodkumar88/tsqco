package com.tsqco.service.impl;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.CacheHelper;
import com.tsqco.helper.DateHelper;
import com.tsqco.models.*;
import com.tsqco.repo.TsqcoAngelAppConfigRepo;
import com.tsqco.repo.TsqcoAngelGainersAndLoserRepo;
import com.tsqco.repo.TsqcoAngelInstrumentsRepo;
import com.tsqco.repo.TsqcoKiteInstrumentsRepo;
import com.tsqco.service.TsqcoDashBoardService;
import com.tsqco.service.TsqcoStockSubscriptionService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Instrument;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.tsqco.constants.TsqcoConstants.*;

@Service
@Slf4j
@AllArgsConstructor
public class TsqcoDashBoardServiceImpl implements TsqcoDashBoardService {

    private final TsqcoProperties tsqcoProps;

    private final TsqcoConfig tsqcoConfig;

    private final CacheHelper cacheHelper;

    private static final long TTL_SECONDS = 8 * 60 * 60;

    private static final String REDIS_KEY_PREFIX = "token:";

    private static final int MAX_POSITIVE_SUBSCRIPTIONS = 15;

    private static final int NEGATIVE_SUBSCRIPTION_COUNT = 1;

    private Map<String, Double> subscribedTokens = new HashMap<>(); // Symbol -> Percentage Change

    @Inject
    @Qualifier("Intrument")
    private final WebClient webClient;

    private final TsqcoKiteInstrumentsRepo tsqcoKiteInstrumentsRepo;

    private final TsqcoAngelInstrumentsRepo tsqcoAngelInstrumentsRepo;

    private final TsqcoAngelGainersAndLoserRepo tsqcoAngelGainersAndLoserRepo;

    private final TsqcoAngelAppConfigRepo tsqcoAngelAppConfigRepo;

    private final RateLimiter rateLimiter;

    private final TsqcoStockSubscriptionService stockSubscriptionService;

    @Override
    public List<Holding> getKitePortfolio() throws KiteException {
        List<Holding> holdings;
        try {
            holdings = tsqcoConfig.getKiteConnect().getHoldings();
        } catch (IOException | KiteException e) {
            log.error("Client is empty {}", e.getMessage());
            throw new KiteException("Client is empty or Session Expired!!");
        }
        return holdings;
    }


    @Override
    public AngelTotalHolding getAngelPortfolio() throws SmartAPIException {
        List<AngelHolding> holdingList = new ArrayList<>();
        AngelTotalHolding angelTotalHolding = null;
        try {
            Object data = tsqcoConfig.getSmartConnect()
                    .getAllHolding().get("data");
            HashMap<String, ArrayList<HashMap>> result =
            new ObjectMapper().readValue( data.toString(), HashMap.class);
            List<HashMap> holdings = result.get("holdings");
            Map<String, Object> totalHoldings = new HashMap<>((Map) result.get("totalholding"));
            for(HashMap<String, Object> map : holdings){
                AngelHolding holding =
                        new AngelHolding(
                                map.get("tradingsymbol").toString(),
                                map.get("exchange").toString(),
                                map.get("isin").toString(),
                                Integer.parseInt(map.get("t1quantity").toString()),
                                Integer.parseInt(map.get("realisedquantity").toString()),
                                Integer.parseInt(map.get("quantity").toString()),
                                Integer.parseInt(map.get("authorisedquantity").toString()),
                                map.get("product").toString(),
                                map.get("collateralquantity") != null ? map.get("collateralquantity").toString() : null,
                                map.get("collateraltype") != null ?map.get("collateraltype").toString() : null,
                                Integer.parseInt(map.get("haircut").toString()),
                                Float.parseFloat(map.get("averageprice").toString()),
                                Float.parseFloat(map.get("ltp").toString()),
                                Integer.parseInt(map.get("symboltoken").toString()),
                                Float.parseFloat(map.get("close").toString()),
                                Float.parseFloat(map.get("profitandloss").toString()),
                                Float.parseFloat(map.get("pnlpercentage").toString()));
                holdingList.add(holding);
            }
            if(holdingList.size() > 0) {
                 angelTotalHolding = new AngelTotalHolding(holdingList,Float.parseFloat(totalHoldings.get("totalholdingvalue").toString()),
                        Float.parseFloat(totalHoldings.get("totalprofitandloss").toString()),
                        Float.parseFloat(totalHoldings.get("totalpnlpercentage").toString()),
                        Float.parseFloat(totalHoldings.get("totalinvvalue").toString()));
            }
        } catch (IOException | SmartAPIException e) {
            log.error("Client is empty {}", e.getMessage());
            throw new SmartAPIException("Client is empty or Session Expired!!");
        }
        return angelTotalHolding;
    }

    @Override
    public void loadAllTheInstruments() throws KiteException, IOException {
        List<Instrument> instruments = new ArrayList<>();
        try {
            instruments = tsqcoConfig.getKiteConnect().getInstruments("NSE");
        } catch (Exception ex ){
            log.error("Exception Occurred while getting all Instruments");
        }
        log.info("Today's Instrument Size {}", instruments.size());
        if(!instruments.isEmpty()) {
            List<TsqcoKiteInstruments> instrumentList = new ArrayList<>();
            for (Instrument instrument : instruments) {
                TsqcoKiteInstruments kiteInstruments = new TsqcoKiteInstruments(instrument);
                instrumentList.add(kiteInstruments);
                if (instrumentList.size() >= TsqcoConstants.BATCH_SIZE) {
                    tsqcoKiteInstrumentsRepo.saveAll(instrumentList);
                }
            }
            tsqcoKiteInstrumentsRepo.saveAll(instrumentList);
        }
    }

    @Override
    public String loadAllAngelInstruments() throws RuntimeException {
        try {
            Optional<AngelApplicationConfig> instrumentLastLoaded = tsqcoAngelAppConfigRepo.findByConfigKey("instrumentlastloaded");
            if (instrumentLastLoaded.isPresent() &&
                    (!instrumentLastLoaded.get().getConfigKey().isEmpty() || !DateHelper.timeRangeCheck(instrumentLastLoaded.get().getConfigValue()))) {
                tsqcoAngelInstrumentsRepo.callBackupAndCleanInstruments();
                tsqcoAngelInstrumentsRepo.truncateInstruments();
                tsqcoAngelInstrumentsRepo.resetInstrumentSequence();
                //tsqcoAngelInstrumentsRepo.manageInstrumentsTable();
                try {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            webClient.get()
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .flatMap(this::processAndSave)
                                    .block(); // Or avoid block() if possible by using asynchronous processing
                        } catch (Exception ex) {
                            log.error("Error in async operation: ", ex);
                            throw new RuntimeException(ex.getMessage());
                        }
                    });
                    // Handle the CompletableFuture result
                    future.exceptionally(throwable -> {
                        log.error("Async operation failed: ", throwable);
                        // Perform any necessary error handling or recovery
                        return null;
                    });
                } catch (Exception ex) {
                    log.error("Error initiating async operation: ", ex);
                    throw new RuntimeException(ex.getMessage());
                }

                AngelApplicationConfig config = instrumentLastLoaded.get();
                config.setConfigValue(LocalDateTime.now().toString());
                tsqcoAngelAppConfigRepo.save(config);
                return INSTRUMENT_LOADED;
            } else {
                log.error("Instrument Config is not present. Please check");
                return INSTRUMENT_NOT_LOADED;
            }
        } catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    private Mono<Void> processAndSave(String jsonData) throws RuntimeException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Set<String> symbolSet = Arrays.stream(tsqcoProps.getFilterSymbol().split(","))
                    .collect(Collectors.toSet());
            List<TsqcoAngelInstruments> instruments = objectMapper.readValue(jsonData, new TypeReference<List<TsqcoAngelInstruments>>(){});
            List<TsqcoAngelInstruments> filteredInstruments = instruments.stream().filter(instrument -> symbolSet.contains(instrument.getExchseg())).collect(Collectors.toList());
            List<TsqcoAngelInstruments> savedInstruments = tsqcoAngelInstrumentsRepo.saveAll(filteredInstruments);
            log.info("Total No of Instruments loaded to DB {} ", filteredInstruments.size());
            List<TsqcoAngelInstruments> filterByEquityInstruments = savedInstruments.stream().filter(e -> e.getSymbol().endsWith("EQ")).collect(Collectors.toList());
            log.info("Total No of Instruments filtered with Equatity {} ", filterByEquityInstruments.size());
            int fetchSize = 50;
            for (int i = 0; i < filterByEquityInstruments.size(); i += fetchSize) {
                int end = Math.min(filterByEquityInstruments.size(), i + fetchSize);
                List<TsqcoAngelInstruments> instrumentsList = filterByEquityInstruments.subList(i, end);
                List<AngelMarketData> marketDataList = getMarketDataWithRetry(instrumentsList);
                Map<String, AngelMarketData> mapOfData = new HashMap<>();
                for(AngelMarketData mktData : marketDataList){
                    mapOfData.put(mktData.getTradingSymbol(), mktData);
                }
                for (TsqcoAngelInstruments instList : instrumentsList) {
                    AngelMarketData mktData = mapOfData.get(instList.getSymbol());
                    if (mktData != null) {
                        instList.setLtp(mktData.getLtp());
                        instList.setPercentagechange(mktData.getPercentChange());
                        instList.setIntrumentdate(LocalDateTime.now());
                    }
                }
                tsqcoAngelInstrumentsRepo.saveAll(instrumentsList);
            }

            log.debug("Total No of Instruments updated with LTP {} ", filterByEquityInstruments.size());
        } catch (Exception e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e.getMessage());
        }
        return Mono.empty();
    }

    public List<AngelMarketData> getMarketDataWithRetry(List<TsqcoAngelInstruments> instrumentsList) throws InterruptedException {
        int attempts = 0;
        List<AngelMarketData> marketData = null;
        int backoff = 2000;
        List<String> tokenList = new ArrayList<>();
        while (attempts < MAX_RETRIES) {
            try {
                rateLimiter.acquire();
                for(TsqcoAngelInstruments tai : instrumentsList) {
                    tokenList.add(tai.getToken());
                }
                marketData = getMarketData(new AngelMarketData("NSE", tokenList, "FULL"), true);
                break; // If the request is successful, break the loop
            } catch (RuntimeException e) {
                attempts++;
                Thread.sleep(backoff);
                backoff *= 2;
                log.error("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    log.error("All attempts failed.");
                }
            }
        }
        return marketData;
    }


    @Override
    public List<AngelMarketData> getMarketData(AngelMarketData mktData, boolean fetchFlag) throws RuntimeException{
        try {
            JSONObject payload = new JSONObject();
            payload.put("mode", mktData.getMode());
            JSONObject exchangeTokens = new JSONObject();
            JSONArray nseTokens = new JSONArray();
            if(fetchFlag) {
                for(String symbolTokens : mktData.getListOfSymbols()){
                    nseTokens.put(symbolTokens);
                }
            } else {
                nseTokens.put(mktData.getSymbolToken());
            }
            exchangeTokens.put(mktData.getExchange(), nseTokens);
            payload.put("exchangeTokens", exchangeTokens);
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<AngelMarketData>> jacksonTypeReference = new TypeReference<List<AngelMarketData>>() {};
            List<AngelMarketData> marketData = objectMapper.readValue(String.valueOf(tsqcoConfig.getSmartConnect()
                    .marketData(payload).get("fetched")), jacksonTypeReference);
            return marketData;
        } catch (SmartAPIException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AngelGainersLosers> getTopGainersAndLosers(String targetDate, int topN, boolean avgFlag) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date parsedDate = dateFormat.parse(targetDate);
        Date sqlDate = new Date(parsedDate.getTime());
        if(avgFlag) {
            return tsqcoAngelGainersAndLoserRepo.getTopGainersAndLosersForLastFiveDays(sqlDate, topN);
        } else {
            return tsqcoAngelGainersAndLoserRepo.getTopGainersAndLosers(sqlDate, topN);
        }
    }

    @Override
    public List<Object[]> findTokenAndLtpBySymbolPattern() {
        return tsqcoAngelInstrumentsRepo.findTokenAndLtpBySymbolPattern("%-EQ");
    }

    @Override
    public void fetchAndStoreTokens() {
        List<Object[]> equityTokens = findTokenAndLtpBySymbolPattern();
        for (Object[] token : equityTokens) {
            // Assuming token[0] is the symbol and token[1] is the LTP
            String symbol = token[0].toString();
            if(token[1] == null){
                log.info(symbol);
            }
            String ltp = (token[1] != null) ? token[1].toString() : "0";

            // Save the token data in Redis with the appropriate key and value
            cacheHelper.saveTokenData(REDIS_KEY_PREFIX + symbol, ltp, TTL_SECONDS);
        }
    }


    @Override
    public void processInitialSubscriptions() throws SmartAPIException, IOException {
        Map<String, Double> tokenChanges = fetchAndCalculatePercentageChanges();

        // Sort by percentage change
        List<Map.Entry<String, Double>> sortedTokens = tokenChanges.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Top 5 positive tokens
        List<Map.Entry<String, Double>> positiveTokens = sortedTokens.stream()
                .filter(entry -> entry.getValue() > 3.0) // Filter tokens with percentage change > 3%
                .collect(Collectors.toList());

        // If no tokens have a percentage change > 3%, take the top 5 tokens
        if (positiveTokens.isEmpty()) {
            positiveTokens = sortedTokens.stream()
                    .limit(5) // Select the top 5 tokens with the highest percentage change
                    .collect(Collectors.toList());
        } else if (positiveTokens.size() < 5) {
            // Find the remaining tokens needed to make the count 5
            int additionalTokensNeeded = 5 - positiveTokens.size();
            // Add the next top tokens from the sorted list that are not already in the positiveTokens list
            List<Map.Entry<String, Double>> finalPositiveTokens = positiveTokens;
            List<Map.Entry<String, Double>> additionalTokens = sortedTokens.stream()
                    .filter(entry -> !finalPositiveTokens.contains(entry)) // Exclude already selected tokens
                    .limit(additionalTokensNeeded) // Select only the required number of additional tokens
                    .collect(Collectors.toList());

            // Combine the positiveTokens with the additionalTokens
            positiveTokens.addAll(additionalTokens);
        } else if (positiveTokens.size() > 5) {
            // Collect all tokens to check LTP in batch
            List<String> tokens = positiveTokens.stream()
                    .map(Map.Entry::getKey) // Extract token keys
                    .collect(Collectors.toList());

            // Fetch LTPs for these tokens in batch
            JSONObject marketData = getMarketDataBatch(String.join(",", tokens)); // Batch API call
            Map<String, Double> ltpMap = extractLtpMap(marketData); // Extract LTP data into a map

            // Initialize LTP threshold
            double ltpThreshold = 500;
            List<Map.Entry<String, Double>> filteredTokens;

            // Gradually increase LTP threshold until we have at least 5 tokens
            do {
                double finalLtpThreshold = ltpThreshold;
                filteredTokens = positiveTokens.stream()
                        .filter(entry -> ltpMap.getOrDefault(entry.getKey(), Double.MAX_VALUE) < finalLtpThreshold) // Apply LTP filter
                        .collect(Collectors.toList());
                ltpThreshold += 500; // Increment LTP threshold
            } while (filteredTokens.size() < 5 && ltpThreshold <= 5000); // Limit the threshold to prevent infinite loop

            // Select up to 5 tokens
            positiveTokens = filteredTokens.stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // 1 token with maximum negative change
        Map.Entry<String, Double> maxNegativeToken = sortedTokens.stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);

        Set<TokenID> tokenSet = new HashSet<>();
        for(Map.Entry<String, Double> token : positiveTokens){
            tokenSet.add(new TokenID(ExchangeType.NSE_CM, token.getKey()));
            subscribedTokens.put(token.getKey(), token.getValue());
        }
        subscribeTokenForStreaming(tokenSet);
        tokenSet = new HashSet<>();

        if (maxNegativeToken != null) {
            tokenSet.add(new TokenID(ExchangeType.NSE_CM, maxNegativeToken.getKey()));
            subscribedTokens.put(maxNegativeToken.getKey(), maxNegativeToken.getValue());
        }
        subscribeTokenForStreaming(tokenSet);
    }

    private Map<String, Double> extractLtpMap(JSONObject marketData) {
        Map<String, Double> ltpMap = new HashMap<>();

        // Extract the 'fetched' array from the market data
        JSONArray fetchedData = marketData.getJSONArray("fetched");

        // Iterate through the 'fetched' array and extract the ltp and tradingSymbol
        for (int i = 0; i < fetchedData.length(); i++) {
            JSONObject tokenData = fetchedData.getJSONObject(i);
            if (tokenData.has("ltp") && tokenData.has("symbolToken")) {
                String symbol = tokenData.getString("symbolToken");
                Double ltp = tokenData.getDouble("ltp");
                ltpMap.put(symbol, ltp);
            }
        }

        return ltpMap;
    }

    private Map<String, Double> fetchAndCalculatePercentageChanges() throws SmartAPIException, IOException {
        // Step 1: Fetch all keys from Redis
        Set<String> keys = cacheHelper.getAllKeys(REDIS_KEY_PREFIX + "*");
        Map<String, Double> tokenChanges = new HashMap<>();

        if (keys.isEmpty()) {
            return tokenChanges; // No keys to process
        }

        // Step 2: Extract symbols from keys
        List<String> symbols = keys.stream()
                .map(key -> key.replace(REDIS_KEY_PREFIX, ""))
                .toList();

        // Step 3: Fetch market data in batches
        JSONObject marketData = getMarketDataBatch(String.join(",", symbols));

        // Step 4: Process market data and calculate percentage changes
        // The response from getMarketDataBatch() contains "fetched" as an array
        JSONArray fetchedData = marketData.getJSONArray("fetched");

        for (Object obj : fetchedData) {
            JSONObject tokenData = (JSONObject) obj;
            String tradingSymbol = tokenData.getString("symbolToken");
            Double newLTP = tokenData.getDouble("ltp");

            // Step 5: Check if this symbol is in the Redis cache
            String redisKey = REDIS_KEY_PREFIX + tradingSymbol;
            String oldLTPStr = cacheHelper.getTokenData(redisKey);

            if (oldLTPStr != null) {
                Double oldLTP = Double.parseDouble(oldLTPStr);

                if (oldLTP != null && newLTP != null) {
                    Double percentageChange = ((newLTP - oldLTP) / oldLTP) * 100;
                    tokenChanges.put(tradingSymbol, percentageChange);
                }
            }
        }
        long elapsedTime = System.currentTimeMillis() - tsqcoConfig.getMarketOpenTime();
        if (elapsedTime >= 60 * 60 * 1000 && elapsedTime < 70 * 60 * 1000) {
                // After filtering and sorting to get top 300 positive percentage tokens
                List<Map.Entry<String, Double>> topTokens = tokenChanges.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(300)
                        .collect(Collectors.toList());

    // Fetch current LTP values for top tokens
                Map<String, String> oldLTPValues = new HashMap<>();
                for (Map.Entry<String, Double> entry : topTokens) {
                    String symbol = entry.getKey();
                    String oldLTP = cacheHelper.getTokenData(REDIS_KEY_PREFIX + symbol);
                    if (oldLTP != null) {
                        oldLTPValues.put(symbol, oldLTP);
                    }
            }

// Clear existing Redis cache
            cacheHelper.removeTokenFromCache(REDIS_KEY_PREFIX + "*");

// Update Redis with old LTP values for retained tokens
            for (Map.Entry<String, Double> entry : topTokens) {
                String symbol = entry.getKey();
                String oldLTP = oldLTPValues.get(symbol);
                if (oldLTP != null) {
                    cacheHelper.saveTokenData(REDIS_KEY_PREFIX + symbol, oldLTP, TTL_SECONDS);
                }
            }
        }

        return tokenChanges;
    }

    @Override
    public JSONObject getMarketDataBatch(String tokensString) throws SmartAPIException, IOException {
        List<String> tokens = Arrays.asList(tokensString.split(",\\s*"));
        JSONObject aggregatedMarketData = new JSONObject();
        int batchSize = 50;

        // Initialize "fetched" as an empty array to accumulate all batch data
        JSONArray fetchedData = new JSONArray();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            JSONObject payload = new JSONObject();
            payload.put("mode", "LTP");

            JSONObject exchangeTokens = new JSONObject();
            JSONArray nseTokens = new JSONArray(batch);
            exchangeTokens.put("NSE", nseTokens);
            payload.put("exchangeTokens", exchangeTokens);

            JSONObject response = getMarketDataWithRetry(payload);

            // Append the "fetched" array from the current response to the accumulated fetchedData
            fetchedData.putAll(response.getJSONArray("fetched"));
        }

        // Put the accumulated data into the aggregatedMarketData
        aggregatedMarketData.put("fetched", fetchedData);

        return aggregatedMarketData;
    }

    private JSONObject getMarketDataWithRetry(JSONObject payload) throws SmartAPIException, IOException {
        int maxRetries = 3;
        long retryDelay = 1000; // Start with 1 second delay

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return tsqcoConfig.getSmartConnect().marketData(payload);
            } catch (IOException e) {
                if (attempt == maxRetries - 1) throw e;
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry", ie);
                }
                retryDelay *= 2; // Exponential backoff
            }
        }
        throw new IOException("Max retries reached");
    }


    /*@Override //Multi Threading Approach
    public JSONObject getMarketDataBatch(String tokensString) throws SmartAPIException, IOException {
        List<String> tokens = Arrays.asList(tokensString.split(",\\s*"));
        JSONObject aggregatedMarketData = new JSONObject();
        int batchSize = 50;

        // Initialize "fetched" as an empty array to accumulate all batch data
        JSONArray fetchedData = new JSONArray();

        List<CompletableFuture<JSONObject>> futures = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            JSONObject payload = new JSONObject();
            payload.put("mode", "LTP");

            JSONObject exchangeTokens = new JSONObject();
            JSONArray nseTokens = new JSONArray(batch);
            exchangeTokens.put("NSE", nseTokens);
            payload.put("exchangeTokens", exchangeTokens);

            CompletableFuture<JSONObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return tsqcoConfig.getSmartConnect().marketData(payload);
                } catch (SmartAPIException | IOException e) {
                    throw new CompletionException(e);
                }
            });

            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.join(); // Wait for all futures to complete

            for (CompletableFuture<JSONObject> future : futures) {
                JSONObject response = future.join();
                fetchedData.putAll(response.getJSONArray("fetched"));
            }
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SmartAPIException) {
                throw (SmartAPIException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new RuntimeException("Unexpected error occurred", e);
            }
        }

        // Put the accumulated data into the aggregatedMarketData
        aggregatedMarketData.put("fetched", fetchedData);

        return aggregatedMarketData;
    }*/



    private void subscribeTokenForStreaming(Set<TokenID> tokenIDs) {
        stockSubscriptionService.addStockSubscription(tokenIDs);
        List<AngelStockSubscription> stockSubscriptions = tokenIDs.stream()
                .map(tokenID -> {
                    AngelStockSubscription subscription = new AngelStockSubscription();
                    subscription.setToken(tokenID.getToken());
                    subscription.setSubscribe(true);
                    return subscription;
                })
                .collect(Collectors.toList());
        stockSubscriptionService.addStockSubscription(stockSubscriptions);
    }

    private void unsubscribeTokenFromStreaming(Set<TokenID> tokenIDs) {
        stockSubscriptionService.removeStockSubscription(tokenIDs);
        tokenIDs.stream().forEach(e -> {
            stockSubscriptionService.deleteByToken(e.getToken());
        });
    }





    @Override
    public void verfiySubsciptions() throws SmartAPIException, IOException {
        long elapsedTime = System.currentTimeMillis() - tsqcoConfig.getMarketOpenTime();

        if (elapsedTime >= 2 * 60 * 60 * 1000) { // Stop after 2 hours
            log.info("Stopping subscription updates after 2 hours.");
            return;
        }

        // Refresh market data
        Map<String, Double> tokenChanges = fetchAndCalculatePercentageChanges();

        // Update subscriptions at defined intervals
        long minutesSinceOpen = elapsedTime / (60 * 1000);

        if (minutesSinceOpen == 5 || minutesSinceOpen % 15 == 0) {
            adjustSubscriptions(tokenChanges);
        }
    }

    private void adjustSubscriptions(Map<String, Double> tokenChanges)  {
        // Remove tokens with negative percentage change or unsubscribed
        subscribedTokens.entrySet().removeIf(entry -> {
            String symbol = entry.getKey();
            Double percentageChange = tokenChanges.getOrDefault(symbol, 0.0);
            boolean shouldRemove = percentageChange <= 0;
            if (shouldRemove && !cacheHelper.isStockPurchased(symbol)) {
                Set<TokenID> unsubscribeSet = new HashSet<>();
                unsubscribeSet.add(new TokenID(ExchangeType.NSE_CM, entry.getKey()));
                unsubscribeTokenFromStreaming(unsubscribeSet);// Pass a single token symbol to unsubscribe
            }
            return shouldRemove;
        });

        // Replace tokens if needed
        int tokensNeeded = MAX_POSITIVE_SUBSCRIPTIONS - subscribedTokens.size();

        if (tokensNeeded > 0) {
            List<Map.Entry<String, Double>> sortedTokens = tokenChanges.entrySet().stream()
                    .filter(entry -> !subscribedTokens.containsKey(entry.getKey())) // Exclude already subscribed
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(tokensNeeded)
                    .collect(Collectors.toList());

            // Prepare a Set of TokenID objects for the batch subscription
            Set<TokenID> tokenSet = new HashSet<>();
            for (Map.Entry<String, Double> token : sortedTokens) {
                tokenSet.add(new TokenID(ExchangeType.NSE_CM, token.getKey()));  // Create TokenID from symbol
                subscribedTokens.put(token.getKey(), token.getValue());  // Store in subscribedTokens map
            }

            subscribeTokenForStreaming(tokenSet);  // Call subscribeToken with the Set of TokenID
        }

        // Add or replace the single maximum negative token
        Map.Entry<String, Double> maxNegativeToken = tokenChanges.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);

        // Check if a new maximum negative token is found and it is different from the current one
        if (maxNegativeToken != null && !subscribedTokens.containsKey(maxNegativeToken.getKey())) {
            String currentMaxNegativeToken = findCurrentMaxNegativeToken();

            if (currentMaxNegativeToken != null && !currentMaxNegativeToken.equals(maxNegativeToken.getKey())) {
                // Unsubscribe the old negative token using Set<TokenID>
                if(!cacheHelper.isStockPurchased(currentMaxNegativeToken)) {
                    Set<TokenID> unsubscribeSet = new HashSet<>();
                    unsubscribeSet.add(new TokenID(ExchangeType.NSE_CM, currentMaxNegativeToken));
                    unsubscribeTokenFromStreaming(unsubscribeSet);
                }

                // Subscribe the new max negative token
                Set<TokenID> subscribeSet = new HashSet<>();
                subscribeSet.add(new TokenID(ExchangeType.NSE_CM, maxNegativeToken.getKey()));
                subscribeTokenForStreaming(subscribeSet);

                // Update the subscribedTokens map
                subscribedTokens.put(maxNegativeToken.getKey(), maxNegativeToken.getValue());
            }
        }
    }

    private String findCurrentMaxNegativeToken() {
        return subscribedTokens.entrySet().stream()
                .min(Map.Entry.comparingByValue()) // Find the entry with the minimum value (most negative)
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
