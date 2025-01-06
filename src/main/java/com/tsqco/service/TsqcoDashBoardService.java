package com.tsqco.service;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.neovisionaries.ws.client.WebSocketException;
import com.tsqco.models.AngelGainersLosers;
import com.tsqco.models.AngelMarketData;
import com.tsqco.models.AngelTotalHolding;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public interface TsqcoDashBoardService {

    List<Holding> getKitePortfolio() throws KiteException;

    AngelTotalHolding getAngelPortfolio() throws SmartAPIException;

    void loadAllTheInstruments() throws KiteException, IOException;

    String  loadAllAngelInstruments() throws RuntimeException;

    List<AngelMarketData> getMarketData(AngelMarketData marketData, boolean fetchFlag);

    List<AngelGainersLosers> getTopGainersAndLosers(String targetDate, int topN, boolean avgFlag) throws ParseException;

    List<Object[]> findTokenAndLtpBySymbolPattern();

    void fetchAndStoreTokens();

    void processInitialSubscriptions() throws SmartAPIException, IOException;

    JSONObject getMarketDataBatch(String tokensString)  throws SmartAPIException, IOException;

    void verfiySubsciptions() throws SmartAPIException, IOException;
}


