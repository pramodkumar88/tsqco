package com.tsqco.service;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.models.AngelGainersLosers;
import com.tsqco.models.AngelMarketData;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.tsqco.models.AngelTotalHolding;
import com.tsqco.models.AngelMarketData;
import com.zerodhatech.models.Holding;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public interface TsqcoDashBoardService {
    List<Holding> getKitePortfolio() throws KiteException;

    AngelTotalHolding getAngelPortfolio() throws SmartAPIException;

    void loadAllTheInstruments() throws KiteException, IOException;

    String  loadAllAngelInstruments() throws InterruptedException;

    List<AngelMarketData> getMarketData(AngelMarketData marketData, boolean fetchFlag);

    List<AngelGainersLosers> getTopGainersAndLosers(String targetDate, int topN, boolean avgFlag) throws ParseException;
}


