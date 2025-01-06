package com.tsqco.helper;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.models.AngelStockSubscription;
import com.tsqco.service.TsqcoComputationService;
import com.tsqco.service.TsqcoDashBoardService;
import com.tsqco.service.TsqcoStockSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tsqco.constants.TsqcoConstants.STOCK_ANALYSIS_CHANNEL;
import static com.tsqco.helper.NotificationHelper.sendMessage;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleManager {

    private final CacheHelper cacheHelper;

    private final StringRedisTemplate redisTemplate;

    private final TsqcoComputationService tsqcoComputationService;

    private final TsqcoStockSubscriptionService tsqcoStockSubscriptionService;

    private final TsqcoDashBoardService tsqcoDashBoardService;

    @Scheduled(cron = "0 */3 9-16 * * MON-FRI")
    public void analyzeBuyStocksPeriodically() {
       try {
           Set<String> tokens = cacheHelper.getKeys("*");
           for (String token : tokens) {
               redisTemplate.convertAndSend(STOCK_ANALYSIS_CHANNEL, token);
           }
       } catch (Exception ex) {
           log.error("analyzeBuyStocksPeriodically : Stock Analysis Failed :: "+ex.getMessage());
           sendMessage(NotificationFormatHelper.jobFailureAlert("STOCK_PERIOD_ANALYSIS", LocalDateTime.now(), "Periodic Analysis Failed. Please check immediately!" ));
       }
    }

    //@Scheduled(cron = "0 45 8 * * MON-FRI") // Runs at every day at 8:45 AM
    public void subscribeStocksForStreaming() throws WebSocketException {
        List<AngelStockSubscription> stockSubscription = tsqcoStockSubscriptionService.getSubscribedStocks();
        log.info("Subscribing All Stocks for Streaming:: "+stockSubscription.size());
        Set<TokenID> tokenSet = new HashSet<>();
        for(AngelStockSubscription subsription : stockSubscription){
            tokenSet.add(new TokenID(ExchangeType.NSE_CM, subsription.getToken()));
        }
        tsqcoStockSubscriptionService.addStockSubscription(tokenSet);
        log.info("Subscribed All Stocks for Streaming!!");
    }

    //@Scheduled(cron = "0 30 16 * * MON-FRI") // Runs everyday at 4 30 PM IST
    public void unSubscribeStocksForStreaming() throws WebSocketException {
        List<AngelStockSubscription> stockSubscription = tsqcoStockSubscriptionService.getSubscribedStocks();
        Set<TokenID> tokenSet = new HashSet<>();
        for(AngelStockSubscription subsription : stockSubscription){
            tokenSet.add(new TokenID(ExchangeType.NSE_CM, subsription.getToken()));
        }
        tsqcoStockSubscriptionService.removeStockSubscription(tokenSet);
        log.info("Unsubscribing All Stocks from Streaming is completed");
    }


    @Scheduled(cron = "0 0 9 * * MON-FRI") // Runs at 9 at once everyday
    public void loadEPSData() throws Exception {
        try {
            tsqcoComputationService.loadEpsData();
            log.info("Loaded EPS Data");
       } catch (RuntimeException ex) {
         sendMessage(NotificationFormatHelper.jobFailureAlert("EPS_JOB", LocalDateTime.now(), "Loading EPS data is Failed!!" ));
         log.error("Loading EPS data is Failed!!");
       }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("EPS_JOB", LocalDateTime.now(), "Loading EPS data is Completed!!" ));
        log.info("Loading EPS data is Completed!!");
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI") // Starts at 9:20 AM
    public void initializeMarketSession() throws SmartAPIException, IOException {
        try {
            tsqcoDashBoardService.fetchAndStoreTokens();
            tsqcoDashBoardService.processInitialSubscriptions();
        } catch (RuntimeException ex) {
            sendMessage(NotificationFormatHelper.jobFailureAlert("INITIALIZE_MARKET_SESSION", LocalDateTime.now(), "Initialization of Market Session Failed!!" ));
            log.error("Initialization of Market Session Failed!!");
        }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("INITIALIZE_MARKET_SESSION", LocalDateTime.now(), "Initialization of Market Session Completed!!" ));
        log.info("Initialization of Market Session Completed!!");
    }

    @Scheduled(cron = "0 30/15 9-11 * * MON-FRI") // Runs every 15 minutes for 2 hours after market opens
    public void analyzeAndUpdateSubscriptions() throws RuntimeException {
        tsqcoDashBoardService.analyzeAndUpdateSubscriptions();
    }


    @Scheduled(cron = "0 45 8 * * MON") // Runs at 8:45 AM onec in a week
    public void loadAllInstruments() {
        try {
            tsqcoDashBoardService.loadAllAngelInstruments();
        } catch (RuntimeException ex) {
            sendMessage(NotificationFormatHelper.jobFailureAlert("ALL_INSTRUMENT_LOAD", LocalDateTime.now(), "Instruments are not refreshed" ));
            log.error("Instruments are not refreshed");
        }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("ALL_INSTRUMENT_LOAD", LocalDateTime.now(), "Insrtuments are refreshed Sucessfully!!" ));
        log.info("Insrtuments are refreshed Sucessfully!!");
    }

    @Scheduled(cron = "0 50 8 * * MON-FRI") // Runs at 8:50 AM everyday once
    public void updateLTPDataOfEquities() {
        try {
            tsqcoDashBoardService.updateLTPOfAllEquityTokens();
        } catch (RuntimeException ex) {
            sendMessage(NotificationFormatHelper.jobFailureAlert("UPDATE_ALL_EQ_LTP", LocalDateTime.now(), "LTP refresh is failed!!" ));
            log.error("LTP refresh is failed!!");
        }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("UPDATE_ALL_EQ_LTP", LocalDateTime.now(), "All LTP are refreshed sucessfully!" ));
        log.info("All LTP are refreshed");
    }

    @Scheduled(cron = "0 50 8 * * MON-FRI") // Runs at 8:50 AM everyday once
    public void clearAllTheTokenFromSubscription()  {
        try {
            tsqcoStockSubscriptionService.deleteAllToken();
            } catch (RuntimeException ex) {
                sendMessage(NotificationFormatHelper.jobFailureAlert("CLEAR_ALL_TOKENS", LocalDateTime.now(), "Tokens are not cleared from Subcription!!" ));
                log.error("All the tokens are not cleared from Subscriptions");
            }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("CLEAR_ALL_TOKENS", LocalDateTime.now(), "All Tokens are cleared from Subcription" ));
        log.info("All the tokens are cleared from Subscriptions");
    }

    @Scheduled(cron = "0 53 8 * * MON-FRI") // Runs at 8:53 AM everyday once
    public void clearRedisCacheData() {
        try {
            cacheHelper.removeAllTokensFromCache();
        } catch (RuntimeException ex) {
            sendMessage(NotificationFormatHelper.jobFailureAlert("CLEAR_DATA_IN_REDIS", LocalDateTime.now(), "All Data is not cleared in the Redis!!" ));
            log.error("Data in the redis is not cleared!!");
        }
        sendMessage(NotificationFormatHelper.jobCompletionAlert("CLEAR_DATA_IN_REDIS", LocalDateTime.now(), "All the data is clear in Redis!" ));
        log.info("All the data is clear in Redis");
    }
}
