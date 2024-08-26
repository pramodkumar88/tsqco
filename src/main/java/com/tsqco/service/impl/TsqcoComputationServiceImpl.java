package com.tsqco.service.impl;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.repo.TsqcoAngelInstrumentsRepo;
import com.tsqco.service.TsqcoComputationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class TsqcoComputationServiceImpl implements TsqcoComputationService {

    private final TsqcoProperties tsqcoProps;

    private final TsqcoConfig tsqcoConfig;

    private final TsqcoAngelInstrumentsRepo tsqcoAngelInstrumentsRepo;

    @Override
    public void getCompute(String tradingSymbol, Date fromDate, Date toDate) throws SmartAPIException, IOException {
        List<TsqcoAngelInstruments> instruments = tsqcoAngelInstrumentsRepo.findDistinctBySymbolStartsWith(tradingSymbol);
        JSONObject payload = new JSONObject();
        payload.put("exchange", "NSE");
        payload.put("searchscrip", "BEL-EQ");
        String response = tsqcoConfig.getSmartConnect().getSearchScrip(payload);

        JSONObject requestObejct = new JSONObject();
        requestObejct.put("exchange", "NSE");
        requestObejct.put("symboltoken", response.split(",")[2].split(":")[1].trim());
        requestObejct.put("interval", "ONE_MINUTE");
        requestObejct.put("fromdate", "2024-08-02 09:00");
        requestObejct.put("todate", "2024-08-02 09:20");

        log.info("Candle Data " +tsqcoConfig.getSmartConnect().candleData(requestObejct));
    }

    @Override
    public List<TsqcoAngelInstruments> getSearchResults(String tradingSymbol, String exSegment) {
       return exSegment.equalsIgnoreCase("ALL")
                ? tsqcoAngelInstrumentsRepo.findDistinctBySymbolStartsWith(tradingSymbol)
                : tsqcoAngelInstrumentsRepo.findDistinctBySymbolStartsWithAndExchseg(tradingSymbol, exSegment);
    }


}
