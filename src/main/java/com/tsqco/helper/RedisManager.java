package com.tsqco.helper;

import com.tsqco.service.TsqcoComputationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.concurrent.TimeUnit;

import static com.tsqco.constants.TsqcoConstants.STOCK_ANALYSIS_CHANNEL;
import static com.tsqco.constants.TsqcoConstants.STOCK_DATA_CHANNEL;
import static com.tsqco.helper.NotificationHelper.sendMessage;

@Component
@AllArgsConstructor
@Slf4j
public class RedisManager implements MessageListener {

    private final StringRedisTemplate redisTemplate;

    private final TsqcoComputationService tsqcoComputationService;

    private static final long TTL_SECONDS = 7500;

    @PostConstruct
    public void init() {
        startListening();
    }
    public void startListening() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(this, new PatternTopic(STOCK_DATA_CHANNEL));
        container.addMessageListener(this, new PatternTopic(STOCK_ANALYSIS_CHANNEL));
        container.afterPropertiesSet();
        container.start();
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(pattern);
        String data = new String(message.getBody());
        //log.info("Received message on channel: " + channel + ", data: " + data);
        if (STOCK_DATA_CHANNEL.equals(channel)) {
            handleStockData(data);
        } else if(STOCK_ANALYSIS_CHANNEL.equals(channel)) {
            analyzeStockData(data);
        } else {
            log.warn("Received message on an unknown channel: " + channel);
        }
    }

    public void handleStockData(String data) {
        try {
            String[] parts = data.split("\\$");
            if (parts.length == 2) {
                String token = parts[0];
                String stockData = parts[1];
                String key = "stockData:" + token;
                redisTemplate.opsForList().leftPush(key, stockData);
                redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception ex){
            log.error("handleStockData :: Exception Occurred : "+ex.getMessage());
            sendMessage(NotificationFormatHelper.genericNotification("Handling Stock Data", ex.getMessage()));
        }
    }

    public void analyzeStockData(String token) {
       try {
           tsqcoComputationService.analyzeStockData(token);
       } catch (Exception ex) {
           log.error("analyzeStockData :: Exception Occurred: "+ex.getMessage());
           sendMessage(NotificationFormatHelper.genericNotification("Analysis Stock Data", ex.getMessage()));
       }
    }

    /*private String fetchStockDataFromCache(String token) {
        String stockData = redisTemplate.opsForList().rightPop("stockData:" + token);
        if (stockData == null) {
            return "No stock data available for token: " + token;
        }
        return stockData;
    }*/
}
