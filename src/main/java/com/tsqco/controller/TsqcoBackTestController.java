package com.tsqco.controller;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.CacheHelper;
import com.tsqco.helper.ScheduleManager;
import com.tsqco.models.dto.AngelBackTestDTO;
import com.tsqco.service.TsqcoComputationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Set;

import static com.tsqco.constants.TsqcoConstants.STOCK_ANALYSIS_CHANNEL;


@RestController
@RequestMapping(value = "/tsqco")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoBackTestController {

    private final TsqcoComputationService computationService;

    private final StringRedisTemplate redisTemplate;

    private final CacheHelper cacheHelper;

    private final ScheduleManager scheduleManager;


    @PostMapping(value="/angel/backtesting", produces = "application/json")
    public void backTest(@RequestBody AngelBackTestDTO backTest) throws SmartAPIException, IOException {
        computationService.getBackTestResult(backTest);
    }

    @GetMapping("/angel/bascktest/buystockrecommendatation")
    public void getBuyStockRecommendation(){
        Set<String> tokens = cacheHelper.getKeys("*");
        for (String token : tokens) {
            redisTemplate.convertAndSend(STOCK_ANALYSIS_CHANNEL,token);
        }
    }

    @GetMapping("/angel/backtest/initlizemarketsession")
    public void initializeMarketSession() throws SmartAPIException, IOException {
        scheduleManager.initializeMarketSession();
    }

    @GetMapping("/angel/backtest/analyzesubscriptions")
    public void analyzeAndUpdateSubscriptions() throws SmartAPIException, IOException {
        scheduleManager.analyzeAndUpdateSubscriptions();
    }

    @GetMapping("/angel/backtest/loadepsdata")
    public void loadEPSData() throws RuntimeException {
        try {
            scheduleManager.loadEPSData();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }


    @GetMapping("/angel/backtest/loadinstruments")
    public void loadAllInstruments() throws RuntimeException {
        try {
            scheduleManager.loadAllInstruments();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @GetMapping("/angel/backtest/updatealltokenltps")
    public void updateAllTokenltps() throws RuntimeException {
        try {
            scheduleManager.updateLTPDataOfEquities();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }


    @GetMapping("/angel/backtest/clearallsubscribedtokens")
    public void clearAllTheTokenFromSubscription()  {
        try {
            scheduleManager.clearAllTheTokenFromSubscription();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @GetMapping("/angel/backtest/clearrediscache")
    public void clearRedisCacheData() {
        try {
            scheduleManager.clearRedisCacheData();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
