package com.tsqco.helper;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tsqco.constants.TsqcoConstants.PURCHASED_STOCK_PREFIX;

@Component
@AllArgsConstructor
public class CacheHelper {

    private final StringRedisTemplate redisTemplate;

    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public void removeAllTokensFromCache() throws RuntimeException {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    public void removeTokenFromCache(String key) {
        redisTemplate.delete(key);
    }


    public String[] getStockData(String token) {
        List<String> stockDataList = redisTemplate.opsForList().range(token, 0, -1);
        if (stockDataList != null) {
            return stockDataList.toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    // Store purchased stock with token, purchase timestamp, and price
    public void storePurchasedStock(String token, double purchasePrice) {
        String key = PURCHASED_STOCK_PREFIX + token;
        Map<String, String> stockData = Map.of(
                "purchasePrice", String.valueOf(purchasePrice),
                "purchaseTime", Instant.now().toString()
        );
        redisTemplate.opsForHash().putAll(key.trim(), stockData);
    }

    // Remove purchased stock from Redis when sold or no longer required
    public void removePurchasedStock(String token) {
        String key = PURCHASED_STOCK_PREFIX + token;
        redisTemplate.delete(key.trim());
    }

    // Retrieve purchased stock data (if exists)
    public Map<Object, Object> getPurchasedStockData(String token) {
        String key = PURCHASED_STOCK_PREFIX + token;
        return redisTemplate.opsForHash().entries(key.trim());
    }

    // Example method to check if stock is purchased
    public boolean isStockPurchased(String token) {
        String key = PURCHASED_STOCK_PREFIX + token;
        return redisTemplate.hasKey(key.trim());
    }

    public void saveTokenData(String key, String value, long ttl) {
        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
    }

    public String getTokenData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteTokenData(String key) {
        redisTemplate.delete(key);
    }

    public Set<String> getAllKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
