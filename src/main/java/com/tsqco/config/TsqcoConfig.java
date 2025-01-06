package com.tsqco.config;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.TokenSet;
import com.angelbroking.smartapi.smartstream.ticker.SmartStreamTicker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.TsqcoDataStreamManager;
import com.tsqco.helper.TsqcoFileService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.*;
import java.util.concurrent.TimeUnit;

import static com.tsqco.constants.TsqcoConstants.*;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TsqcoConfig {

    private final TsqcoProperties tsqcoProps;

    private final TsqcoFileService tsqcoFileService;

    private static final int MAX_RETRIES = 5;

    private static final long BACKOFF_INITIAL_DELAY = 2000; // Initial backoff delay in milliseconds

    private static final long BACKOFF_MULTIPLIER = 2;

    private final ObjectMapper objectMapper;

   @Value("${telegram.appURL}")
    public String telegramAppURL;

    @Value("${ollama.appURL}")
    public String ollamaAppURL;

    @Value("${angel.appURL.margincalculator}")
    public String angelMarginURL;



    @Lazy
    @Bean
    public KiteConnect getKiteConnect() throws KiteException {
        KiteConnect kiteSdk = new KiteConnect(tsqcoProps.getKiteApiKey());
        kiteSdk.setUserId(tsqcoProps.getKiteUserId());
        User user = null;
        try {
            user =  kiteSdk.generateSession(tsqcoProps.getKiteRequestToken(), tsqcoProps.getKiteSecretKey());
            kiteSdk.setAccessToken(user.accessToken);
            kiteSdk.setPublicToken(user.publicToken);
            kiteSdk.setSessionExpiryHook(() -> log.info("Session Expired. Re-Login!!"));
        } catch (Exception ex) {
            log.error("Session is expired {}", ex);
            throw new KiteException("Session is expired");
        }
        return kiteSdk;
    }


    @Bean
    @Order(1)
    public SmartConnect getSmartConnect() {
        SmartConnect smartConnect = new SmartConnect(ANGEL_API_KEY);
        try {
            com.angelbroking.smartapi.models.User userWithRetry = getUserWithRetry(smartConnect);
            tsqcoFileService.writeToFile(BASE_DIR + TMP_FILE_NAME,
                    userWithRetry.accessToken + "," + userWithRetry.refreshToken);
            FEED_TOKEN = userWithRetry.getFeedToken();
            smartConnect.setAccessToken(userWithRetry.accessToken);
            smartConnect.setRefreshToken(userWithRetry.refreshToken);
        } catch (Exception ex) {
            log.error("Session expired or error occurred: " + ex);
            // Attempt to renew the access token with retries
            try {
                renewTokenWithRetry(smartConnect);
            } catch (Exception e) {
                log.error("renewTokenWithRetry!!"+e);
            }
        }
        smartConnect.setSessionExpiryHook(() -> log.info("Session Expired. Re-Login!!"));
        return smartConnect;
    }

    public void renewTokenWithRetry(SmartConnect smartConnect) throws InterruptedException {
        int attempts = 0;
        long backoff = BACKOFF_INITIAL_DELAY;
        boolean renewed = false;
        while (attempts < MAX_RETRIES) {
            try {
                String tokens = tsqcoFileService.readFromFile(BASE_DIR + TMP_FILE_NAME);
                String[] token = tokens.split(",");
                TokenSet tokenSet = smartConnect.renewAccessToken(token[0], token[1]);
                smartConnect.setAccessToken(tokenSet.accessToken);
                smartConnect.setRefreshToken(tokenSet.refreshToken);
                log.debug("Access Token Successfully renewed");
                renewed = true;
                break;
            } catch (Exception e) {
                attempts++;
                Thread.sleep(backoff);
                backoff *= BACKOFF_MULTIPLIER;
                log.error("Token renewal attempt " + attempts + " failed: " + e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    log.error("All token renewal attempts failed.");
                    throw new RuntimeException("Failed to renew token after several attempts", e);
                }
            }
        }
        if (!renewed) {
            throw new RuntimeException("Token renewal failed after multiple attempts");
        }
    }


    public com.angelbroking.smartapi.models.User getUserWithRetry(SmartConnect smartConnect) throws Exception {
        int attempts = 0;
        long backoff = BACKOFF_INITIAL_DELAY;
        com.angelbroking.smartapi.models.User user = null;

        while (attempts < MAX_RETRIES) {
            try {
                // Try generating the user session (this method can throw SmartAPIException)
                user = getUser(smartConnect);

                // If successful, return the user and break out of the loop
                if (user != null) {
                    return user;
                }
            } catch (Exception ex) {
                // General exception handler
                log.error("Unexpected exception: " + ex.getMessage());
                // Handle retries on any exception (including SmartAPIException)
                attempts++;
            }

            // Retry logic with exponential backoff
            if (attempts >= MAX_RETRIES) {
                log.error("Failed to generate session after " + MAX_RETRIES + " attempts.");
                throw new RuntimeException("Failed to generate session after multiple attempts.");
            }

            log.info("Retrying after " + backoff + "ms...");
            Thread.sleep(backoff);
            backoff *= BACKOFF_MULTIPLIER; // Increase the backoff for the next attempt
        }

        return user; // Return user if successfully created, otherwise null after retries
    }

    public com.angelbroking.smartapi.models.User getUser(SmartConnect smartConnect) {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        int attempts = 0;
        long backoff = BACKOFF_INITIAL_DELAY;
        com.angelbroking.smartapi.models.User user = null;
        String totp = String.valueOf(gAuth.getTotpPassword(ANGEL_TOTP_KEY));
        while (attempts < MAX_RETRIES) {
            try {
                // Attempt to generate session with TOTP
                user = smartConnect.generateSession(ANGEL_CLIENT_ID, "3419", totp);
                break; // Break if successful
            } catch (Exception ex) {
                /*if ("Invalid totp".equals(ex.getMessage())) {
                    log.error("Invalid TOTP. Regenerating and retrying...");
                    String newTotp = String.valueOf(gAuth.getTotpPassword(ANGEL_TOTP_KEY));
                    // Generate new TOTP and retry
                }*/
                // Catch any exception and retry if necessary
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("All attempts to generate user session failed.");
                    throw ex; // Re-throw exception if all retries failed
                }
                log.error("Attempt " + attempts + " failed: " + ex.getMessage());
                log.info("Retrying after backoff...");
                try {
                    Thread.sleep(backoff); // Apply backoff before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                backoff *= BACKOFF_MULTIPLIER; // Increase backoff for next retry
            }
        }

        return user;
    }




    @Bean
    public HttpClient getHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

    }

    @Bean
    public RateLimiter getRateLimiter() {
        return RateLimiter.create(8.0);
    }

    @Bean
    @Primary
    public WebClient getWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(getHttpClient()))
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                .maxInMemorySize(25000 * 1024))
                        .build())
                .build();
    }

    @Bean(name = "Intrument")
    public WebClient getInstrumentWebClient(WebClient.Builder builder) {
        return builder.baseUrl(angelMarginURL).build();
    }

    @Bean(name = "Notification")
    public WebClient getNotificationWebClient() {
       return WebClient.builder()
                .baseUrl(telegramAppURL)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(30))  // Increase the response timeout
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // Set connection timeout
                ))
                .build();
    }

    @Bean(name = "Analysis")
    public WebClient getStockAnalysisWebClient(WebClient.Builder builder) {
        return builder.baseUrl(ollamaAppURL).build();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }


    /*@Bean
    public long getMarketOpenTime() {
        LocalTime marketOpenTime = LocalTime.of(16,0);
        ZonedDateTime marketOpenDateTime = ZonedDateTime.of(LocalDate.now(), marketOpenTime, ZoneId.of("Asia/Kolkata"));
        return marketOpenDateTime.toInstant().toEpochMilli();
    }*/


    @Bean
    @Lazy
    public SmartStreamTicker smartStreamTicker(StringRedisTemplate redisTemplate) throws WebSocketException {
        String clientCode = TsqcoConstants.ANGEL_CLIENT_ID;
        String feedToken = FEED_TOKEN;

        TsqcoDataStreamManager customListener = new TsqcoDataStreamManager(redisTemplate);
        SmartStreamTicker smartStreamTicker = new SmartStreamTicker(clientCode, feedToken, customListener);

        // Start a background thread to manage the connection
        new Thread(() -> manageWebSocketConnection(smartStreamTicker)).start();

        return smartStreamTicker;
    }

    private void manageWebSocketConnection(SmartStreamTicker smartStreamTicker) {
        while (true) {
            try {
                if (!smartStreamTicker.isConnectionOpen()) {
                    smartStreamTicker.connect();
                    log.info("WebSocket connected successfully");
                }
                Thread.sleep(5000); // Check connection every 5 seconds
            } catch (Exception e) {
                log.error("Error in WebSocket connection: " + e.getMessage());
                try {
                    Thread.sleep(5000); // Wait before attempting to reconnect
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }




    public <T> T mapJsonToObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            System.err.println("Error mapping JSON to object: " + e.getMessage());
            return null;
        }
    }
}


