package com.tsqco.controller;

import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.SmartStreamSubsMode;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.ScheduleManager;
import com.tsqco.models.AngelStockSubscription;
import com.tsqco.service.TsqcoStockSubscriptionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tsqco.helper.NotificationHelper.sendMessage;

@RestController
@RequestMapping(value = "/tsqco")
@AllArgsConstructor
@Slf4j
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoStockController {

    private final TsqcoStockSubscriptionService tsqcoStockSubscriptionService;

    private final StringRedisTemplate redisTemplate;

    private final TsqcoConfig tsqcoConfig;

    private final ScheduleManager scheduleManager;

    @GetMapping("/subscribed")
    public ResponseEntity<List<AngelStockSubscription>> getSubscribedStocks() {
        List<AngelStockSubscription> stocks = tsqcoStockSubscriptionService.getSubscribedStocks();
        return ResponseEntity.ok(stocks);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> addStockSubscription(@RequestBody List<AngelStockSubscription> stockSubscription) throws WebSocketException {
        String savedStockStatus = tsqcoStockSubscriptionService.addStockSubscription(stockSubscription);
        Set<TokenID> tokenSet = new HashSet<>();
         for(AngelStockSubscription subsription : stockSubscription){
             tokenSet.add(new TokenID(ExchangeType.NSE_CM, subsription.getToken()));
         }
         tsqcoStockSubscriptionService.addStockSubscription(tokenSet);
        return ResponseEntity.ok(savedStockStatus);
    }

    @DeleteMapping("/unsubscribe/{token}")
    public ResponseEntity<Void> deleteByToken(@PathVariable String token) {
        Set<TokenID> tokenSet = new HashSet<>();
        try {
            tsqcoStockSubscriptionService.deleteByToken(token);
            tokenSet.add(new TokenID(ExchangeType.NSE_CM, token));
            tsqcoConfig.smartStreamTicker(redisTemplate).unsubscribe(SmartStreamSubsMode.SNAP_QUOTE, tokenSet);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException | WebSocketException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/unsubscribeall")
    public ResponseEntity<Void> deleteAll() {
        try {
            tsqcoStockSubscriptionService.deleteAllToken();
            List<AngelStockSubscription> stockSubscription = tsqcoStockSubscriptionService.getSubscribedStocks();
            try {
                boolean isConnectionOpen = tsqcoConfig.smartStreamTicker(redisTemplate).isConnectionOpen();
                if(isConnectionOpen){
                    Set<TokenID> tokenSet = new HashSet<>();
                    for(AngelStockSubscription subsription : stockSubscription){
                        tokenSet.add(new TokenID(ExchangeType.NSE_CM, subsription.getToken()));
                    }
                    tsqcoConfig.smartStreamTicker(redisTemplate).unsubscribe(SmartStreamSubsMode.SNAP_QUOTE,tokenSet);
                }
            } catch (Exception ex ){
                log.error("Error Occurred"+ex);
            }
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/streamalltokens")
    public void subscribeStockToken() throws WebSocketException {
        scheduleManager.subscribeStocksForStreaming();
    }

    @PostMapping("/unstreamalltokens")
    public void unsubscribeStockToken() throws WebSocketException {
        scheduleManager.unSubscribeStocksForStreaming();
    }
}


