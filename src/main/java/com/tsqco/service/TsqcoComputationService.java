package com.tsqco.service;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.TsqcoInstrumentDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface TsqcoComputationService {

    void getCompute(String tradingsymbol, Date fromDate, Date toDate) throws SmartAPIException, IOException;

    List<TsqcoAngelInstruments> getSearchResults(String tradingSymbol, String exSegment);
}

