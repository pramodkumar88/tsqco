package com.tsqco.helper;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.smartstream.models.*;
import com.angelbroking.smartapi.smartstream.ticker.SmartStreamListener;
import com.tsqco.constants.TsqcoConstants;
import io.lettuce.core.XTrimArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.tsqco.helper.NotificationHelper.sendMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class TsqcoDataStreamManager implements SmartStreamListener {

    public static final ZoneId TZ_IST = ZoneId.of("Asia/Kolkata");

    private final StringRedisTemplate redisTemplate;

    @Override
    public void onLTPArrival(LTP ltp) {
        String ltpData = String.format(
                "subscriptionMode: %s exchangeType: %s token: %s sequenceNumber: %d ltp: %.2f exchangeTime: %s exchangeToClientLatency: %s",
                SmartStreamSubsMode.findByVal(ltp.getSubscriptionMode()),
                ltp.getExchangeType(), ltp.getToken().toString(), ltp.getSequenceNumber(),
                (ltp.getLastTradedPrice() / 100.0), getExchangeTime(ltp.getExchangeFeedTimeEpochMillis()),
                Instant.now().toEpochMilli() - ltp.getExchangeFeedTimeEpochMillis());
        System.out.println(ltpData);
    }

    @Override
    public void onQuoteArrival(Quote quote) {
        String data = String.format("subscriptionMode: %s token: %s"
                        //+ " sequenceNumber: %d"
                        + " ltp: %.2f"
                        + " open: %.2f"
                        + " high: %.2f"
                        + " low: %.2f"
                        + " close: %.2f"
                        + " exchangeTime: %s",
                        //+ " exchangeToClientLatency: %s",
                SmartStreamSubsMode.findByVal(quote.getSubscriptionMode()),
                quote.getToken().toString().trim(),
                //quote.getSequenceNumber(),
                (quote.getLastTradedPrice() / 100.0),
                (quote.getOpenPrice() / 100.0),
                (quote.getHighPrice() / 100.0),
                (quote.getLowPrice() / 100.0),
                (quote.getClosePrice() / 100.0),
                getExchangeTime(quote.getExchangeFeedTimeEpochMillis()) //,
                //Instant.now().toEpochMilli() - quote.getExchangeFeedTimeEpochMillis()
                );
        //System.out.println(data);
        publishStockData(quote.getToken().toString().trim(),data);
    }

    @Override
    public void onSnapQuoteArrival(SnapQuote snapQuote) {
        double ltp = Math.round((snapQuote.getLastTradedPrice() / 100.0) * 100.0) / 100.0;
        double open = Math.round((snapQuote.getOpenPrice() / 100.0) * 100.0) / 100.0;
        double high = Math.round((snapQuote.getHighPrice() / 100.0) * 100.0) / 100.0;
        double low = Math.round((snapQuote.getLowPrice() / 100.0) * 100.0) / 100.0;
        double close = Math.round((snapQuote.getClosePrice() / 100.0) * 100.0) / 100.0;
        double upperCircuit = Math.round((snapQuote.getUpperCircuit() / 100.0) * 100.0) / 100.0;
        double lowerCircuit = Math.round((snapQuote.getLowerCircuit() / 100.0) * 100.0) / 100.0;
        double yearlyHighPrice = Math.round((snapQuote.getYearlyHighPrice() / 100.0) * 100.0) / 100.0;
        double yearlyLowPrice = Math.round((snapQuote.getYearlyLowPrice() / 100.0) * 100.0) / 100.0;
        double openInterest = Math.round((snapQuote.getOpenInterest() / 100.0) * 100.0) / 100.0;

        String snapQuoteData = String.format(
                "subscriptionMode: %s token: %s ltp: %.2f open: %.2f high: %.2f low: %.2f close: %.2f exchangeTime: %s upperCircuit: %.2f lowerCircuit: %.2f yearlyHighPrice: %.2f yearlyLowPrice: %.2f "
                        + "lastTradedQty: %d avgTradedPrice: %.2f volumeTradedToday: %d totalBuyQty: %.2f totalSellQty: %.2f openInterest: %.2f openInterestChangePerc: %.2f bestFiveBuyData: %s bestFiveSellData: %s ",
                SmartStreamSubsMode.findByVal(snapQuote.getSubscriptionMode()),
                snapQuote.getToken().toString().trim(),
                ltp, open, high, low, close, getExchangeTime(snapQuote.getExchangeFeedTimeEpochMillis()),
                upperCircuit, lowerCircuit, yearlyHighPrice, yearlyLowPrice,
                snapQuote.getLastTradedQty(), Math.round((snapQuote.getAvgTradedPrice() / 100.0) * 100.0) / 100.0,
                snapQuote.getVolumeTradedToday(), snapQuote.getTotalBuyQty(),
                snapQuote.getTotalSellQty(), openInterest,
                snapQuote.getOpenInterestChangePerc(), Arrays.toString(snapQuote.getBestFiveBuy()),
                Arrays.toString(snapQuote.getBestFiveSell()));
        //log.info("Snap Quota :: "+snapQuoteData);
        publishStockData(snapQuote.getToken().toString().trim(),snapQuoteData);
    }

    private ZonedDateTime getExchangeTime(long exchangeFeedTimeEpochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(exchangeFeedTimeEpochMillis), TZ_IST);
    }


    @Override
    public void onDepthArrival(Depth depth) {
        String depthData = String.format(
                "subscriptionMode: %s exchangeType: %s token: %s exchangeTimeStamp: %s packetReceivedTime: %s bestTwentyBuyData: %s bestTwentySellData: %s",
                SmartStreamSubsMode.findByVal(depth.getSubscriptionMode()),
                depth.getExchangeType(), depth.getToken().toString(),
                depth.getExchangeTimeStamp(),
                depth.getPacketReceivedTime(),
                Arrays.toString(depth.getBestTwentyBuyData()),
                Arrays.toString(depth.getBestTwentySellData()));
        publishStockData(depth.getToken().toString(),depthData);
    }

    @Override
    public void onConnected() {
        log.info("WebSocket Connection is sucessfull!!");
    }

    @Override
    public void onDisconnected() {
        log.info("WebSocket Connection is disconnected!!");
    }

    @Override
    public void onError(SmartStreamError smartStreamError) {
        smartStreamError.getException().printStackTrace();
        sendMessage("\uD83D\uDD34 *CRITICAL: Application is Down* \uD83D\uDD34  \n" +
                "Smart Streaming is disconnected. Kindly check!!  \n" +
                "Timestamp: " + LocalDateTime.now()+ " \n" +
                "Action: check service is running.");
    }

    @Override
    public void onPong() {
        //log.info("pong received");
    }

    @Override
    public SmartStreamError onErrorCustom() {
        SmartStreamError smartStreamError = new SmartStreamError();
        smartStreamError.setException(new SmartAPIException("custom error received"));
        return smartStreamError;
    }

    // Method to publish stock data
    public void publishStockData(String token, String stockData) {
        String message = token + "$" + stockData;
        //log.info("Stream:: "+message);
        redisTemplate.convertAndSend(TsqcoConstants.STOCK_DATA_CHANNEL, message.trim());
        //redisTemplate.opsForList().leftPush("stockData:" + token, stockData);
    }

}