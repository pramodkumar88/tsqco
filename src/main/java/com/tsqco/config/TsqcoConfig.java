package com.tsqco.config;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.TokenSet;
import com.google.common.util.concurrent.RateLimiter;
import com.tsqco.helper.CookieHelper;
import com.tsqco.helper.TsqcoFileService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.tsqco.constants.TsqcoConstants.*;

@Configuration
@Slf4j
@AllArgsConstructor
public class TsqcoConfig {

    private final TsqcoProperties tsqcoProps;

    private final TsqcoFileService tsqcoFileService;


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
    public SmartConnect getSmartConnect() throws SmartAPIException {
        SmartConnect smartConnect = new SmartConnect(ANGEL_API_KEY);
        com.angelbroking.smartapi.models.User user = null;
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        try {
            com.angelbroking.smartapi.models.User userWithRetry = getUserWithRetry(gAuth, smartConnect);
            tsqcoFileService.writeToFile(BASE_DIR+TMP_FILE_NAME,
                    userWithRetry.accessToken+","+userWithRetry.refreshToken );
            FEED_TOKEN = userWithRetry.getFeedToken();
            smartConnect.setAccessToken(userWithRetry.accessToken);
            smartConnect.setRefreshToken(userWithRetry.refreshToken);

        } catch (Exception ex) {
            log.error("Session is expired !! {}", ex);
            String tokens = tsqcoFileService.readFromFile(BASE_DIR + TMP_FILE_NAME);
            String token[] = tokens.split(",");
            TokenSet tokenSet = smartConnect.renewAccessToken(token[0], token[1]);
            smartConnect.setAccessToken(tokenSet.accessToken);
            smartConnect.setRefreshToken(tokenSet.refreshToken);
            log.debug("Access Token Successfully renewed");
            return smartConnect;
        }
        smartConnect.setSessionExpiryHook(() ->
                log.info("Session Expired. Re-Login!!"));
        return  smartConnect;
    }


    public com.angelbroking.smartapi.models.User getUserWithRetry(GoogleAuthenticator gAuth, SmartConnect smartConnect)
            throws SmartAPIException, InterruptedException {
        int attempts = 0;
        int backoff = 2000;
        com.angelbroking.smartapi.models.User user = null;
        while (attempts < MAX_RETRIES) {
            try {
                user = getUser(gAuth, smartConnect);
                break; // If the request is successful, break the loop
            } catch (Exception e) {
                attempts++;
                Thread.sleep(backoff);
                backoff *= 2;
                log.error("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    log.error("All attempts failed.");
                }
            }
        }
        return user;
    }

    public com.angelbroking.smartapi.models.User getUser(GoogleAuthenticator gAuth, SmartConnect smartConnect) throws SmartAPIException {
        return smartConnect.generateSession(ANGEL_CLIENT_ID, "3419",
                String.valueOf(gAuth.getTotpPassword(ANGEL_TOTP_KEY)));
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

    @Bean(name = "WebclientCodec")
    public WebClient getWebClient() {
       return WebClient.builder().baseUrl("https://margincalculator.angelbroking.com")
                .clientConnector(new ReactorClientHttpConnector(getHttpClient()))
               .exchangeStrategies(ExchangeStrategies
                       .builder()
                       .codecs(codecs -> codecs
                               .defaultCodecs()
                               .maxInMemorySize(25000 * 1024))
                       .build())
                .build();
    }

}


