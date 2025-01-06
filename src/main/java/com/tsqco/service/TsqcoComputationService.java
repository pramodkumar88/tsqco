package com.tsqco.service;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TsqcoComputationService {

    List<AngelCandleStickResponseDTO> getCandleStickData(AngelCandleStickRequestDTO candleStickDataDTO) throws SmartAPIException, IOException;

    List<TsqcoAngelInstruments> getSearchResults(String tradingSymbol, String exSegment);

    void getBackTestResult(AngelBackTestDTO backTest) throws SmartAPIException, IOException;

    void loadEpsData() throws Exception;

    String analyzeBuyorHoldStock(Map<String, List<String>> stockData);

    String analyzeSellorHoldStock(Map<String, List<String>> stockData);

    void handleRecommendation(AngelRecommendationDataDTO recommendationData);

    public void analyzeStockData(String token);
}

