package com.tsqco.service.impl;

import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.SmartStreamSubsMode;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.neovisionaries.ws.client.WebSocketError;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.helper.NotificationFormatHelper;
import com.tsqco.helper.NotificationHelper;
import com.tsqco.models.AngelStockSubscription;
import com.tsqco.repo.TsqcoAngelStockSubscriptionRepo;
import com.tsqco.service.TsqcoStockSubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.tsqco.helper.NotificationHelper.sendMessage;

@Service
@AllArgsConstructor
@Slf4j
public class TsqcoStockSubscriptionServiceImpl implements TsqcoStockSubscriptionService {

    private final TsqcoAngelStockSubscriptionRepo tsqcoAngelStockSubscriptionRepo;

    private final TsqcoConfig tsqcoConfig;

    private final StringRedisTemplate redisTemplate;

    public List<AngelStockSubscription> getSubscribedStocks() {
        return tsqcoAngelStockSubscriptionRepo.findBySubscribeTrue();
    }

    public String addStockSubscription(List<AngelStockSubscription> stockSubscription) {
        List<AngelStockSubscription> savedStocks = new ArrayList<>();
        try {
            for (AngelStockSubscription angelStockSubscription : stockSubscription) {
                savedStocks.add(tsqcoAngelStockSubscriptionRepo.save(angelStockSubscription));
            }
        } catch (Exception ex) {
            if(ex instanceof DataIntegrityViolationException) {
                return "Stock is already subscribed";
            }

        }
        return savedStocks.size() + " : Stocks are added to Subscribe!!";
    }

    public AngelStockSubscription updateStockSubscription(Long id, boolean subscribe) {
        Optional<AngelStockSubscription> optionalStock = tsqcoAngelStockSubscriptionRepo.findById(id);

        if (optionalStock.isPresent()) {
            AngelStockSubscription stock = optionalStock.get();
            stock.setSubscribe(subscribe); // Set the subscription status to false
            return tsqcoAngelStockSubscriptionRepo.save(stock); // Save the updated stock back to the database
        }
        return null;
    }

    @Transactional
    public void deleteByToken(String token) {
        if (tsqcoAngelStockSubscriptionRepo.existsByToken(token)) {
            tsqcoAngelStockSubscriptionRepo.deleteByToken(token);
        } else {
            throw new RuntimeException("Stock subscription not found with token: " + token);
        }
    }

    @Override
    public void deleteAllToken() throws RuntimeException {
        tsqcoAngelStockSubscriptionRepo.deleteAll();
    }

    @Override
    public void addStockSubscription(Set<TokenID> tokenIDS) {
        try {
            boolean isConnectionOpen = tsqcoConfig.smartStreamTicker(redisTemplate).isConnectionOpen();
            if(isConnectionOpen){
                tsqcoConfig.smartStreamTicker(redisTemplate).subscribe(SmartStreamSubsMode.SNAP_QUOTE,tokenIDS);
            }
        } catch (Exception ex ){
            log.error("Some error occurred during unsubscribing the stock "+ex.getMessage());
            sendMessage(NotificationFormatHelper
                    .tokenUnsubscriptionFailureAlert(tokenIDS.toString(),ex.getMessage(), LocalDateTime.now(),""));
        }
    }

    @Override
    public void removeStockSubscription(Set<TokenID> tokenIDS) {
        try {
            boolean isConnectionOpen = tsqcoConfig.smartStreamTicker(redisTemplate).isConnectionOpen();
            if(isConnectionOpen){
                tsqcoConfig.smartStreamTicker(redisTemplate).unsubscribe(SmartStreamSubsMode.SNAP_QUOTE,tokenIDS);
            }
        } catch (Exception ex ){
            log.error("Some error occurred during unsubscribing the stock "+ex.getMessage());
               sendMessage(NotificationFormatHelper
                       .tokenUnsubscriptionFailureAlert(tokenIDS.toString(),ex.getMessage(), LocalDateTime.now(),""));
        }
    }


}
