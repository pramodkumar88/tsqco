package com.tsqco.controller;


import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.CacheHelper;
import com.tsqco.helper.ScheduleManager;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.AngelBackTestDTO;
import com.tsqco.models.dto.AngelCandleStickRequestDTO;
import com.tsqco.models.dto.AngelCandleStickResponseDTO;
import com.tsqco.service.TsqcoComputationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.tsqco.constants.TsqcoConstants.STOCK_ANALYSIS_CHANNEL;

@RestController
@RequestMapping(value = "/tsqco")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoComputationController {

    private final TsqcoComputationService computationService;

    private final StringRedisTemplate redisTemplate;

    private final CacheHelper cacheHelper;

    private final ScheduleManager scheduleManager;

    @PostMapping(value = "/angel/candlestickdata", produces = "application/json")
    public List<AngelCandleStickResponseDTO> getCandleStickData(@RequestBody AngelCandleStickRequestDTO candleStickRequest) throws SmartAPIException, IOException {
        return computationService.getCandleStickData(candleStickRequest);
    }

    @GetMapping(value = "/angel/search", produces = "application/json")
    public  List<TsqcoAngelInstruments> searchResult(@RequestParam String tradingsymbol,
                                                     @RequestParam(defaultValue = "ALL") String exsegment) {
        return computationService.getSearchResults(tradingsymbol.toUpperCase(), exsegment.toUpperCase());
    }


    @PostMapping(value="/angel/backtesting", produces = "application/json")
    public void backTest(@RequestBody AngelBackTestDTO backTest) throws SmartAPIException, IOException {
        computationService.getBackTestResult(backTest);
    }

    @GetMapping("/angel/loadepsdata")
    public String loadEPSData() throws Exception {
        try {
            computationService.loadEpsData();
        } catch (Exception ex) {
            return "Error Occurred: "+ex.getMessage();
        }
        return "EPS Data sucessfully loaded";
    }

    @GetMapping("/angel/buystockrecommendatation")
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

}
