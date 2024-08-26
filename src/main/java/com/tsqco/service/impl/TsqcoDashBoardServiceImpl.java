package com.tsqco.service.impl;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.tsqco.config.TsqcoConfig;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.helper.DateHelper;
import com.tsqco.models.*;
import com.tsqco.repo.TsqcoAngelAppConfigRepo;
import com.tsqco.repo.TsqcoAngelGainersAndLoserRepo;
import com.tsqco.repo.TsqcoAngelInstrumentsRepo;
import com.tsqco.repo.TsqcoKiteInstrumentsRepo;
import com.tsqco.service.TsqcoDashBoardService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Instrument;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tsqco.constants.TsqcoConstants.*;

@Service
@Slf4j
@AllArgsConstructor
public class TsqcoDashBoardServiceImpl implements TsqcoDashBoardService {

    private final TsqcoProperties tsqcoProps;

    private final TsqcoConfig tsqcoConfig;

    private final WebClient webClient;

    private final TsqcoKiteInstrumentsRepo tsqcoKiteInstrumentsRepo;

    private final TsqcoAngelInstrumentsRepo tsqcoAngelInstrumentsRepo;

    private final TsqcoAngelGainersAndLoserRepo tsqcoAngelGainersAndLoserRepo;

    private final TsqcoAngelAppConfigRepo tsqcoAngelAppConfigRepo;

    private final RateLimiter rateLimiter;

    @Override
    public List<Holding> getKitePortfolio() throws KiteException {
        List<Holding> holdings;
        try {
            holdings = tsqcoConfig.getKiteConnect().getHoldings();
        } catch (IOException | KiteException e) {
            log.error("Client is empty {}", e.getMessage());
            throw new KiteException("Client is empty or Session Expired!!");
        }
        return holdings;
    }


    @Override
    public AngelTotalHolding getAngelPortfolio() throws SmartAPIException {
        List<AngelHolding> holdingList = new ArrayList<>();
        AngelTotalHolding angelTotalHolding = null;
        try {
            Object data = tsqcoConfig.getSmartConnect()
                    .getAllHolding().get("data");
            HashMap<String, ArrayList<HashMap>> result =
            new ObjectMapper().readValue( data.toString(), HashMap.class);
            List<HashMap> holdings = result.get("holdings");
            Map<String, Object> totalHoldings = new HashMap<>((Map) result.get("totalholding"));
            for(HashMap<String, Object> map : holdings){
                AngelHolding holding =
                        new AngelHolding(
                                map.get("tradingsymbol").toString(),
                                map.get("exchange").toString(),
                                map.get("isin").toString(),
                                Integer.parseInt(map.get("t1quantity").toString()),
                                Integer.parseInt(map.get("realisedquantity").toString()),
                                Integer.parseInt(map.get("quantity").toString()),
                                Integer.parseInt(map.get("authorisedquantity").toString()),
                                map.get("product").toString(),
                                map.get("collateralquantity") != null ? map.get("collateralquantity").toString() : null,
                                map.get("collateraltype") != null ?map.get("collateraltype").toString() : null,
                                Integer.parseInt(map.get("haircut").toString()),
                                Float.parseFloat(map.get("averageprice").toString()),
                                Float.parseFloat(map.get("ltp").toString()),
                                Integer.parseInt(map.get("symboltoken").toString()),
                                Float.parseFloat(map.get("close").toString()),
                                Float.parseFloat(map.get("profitandloss").toString()),
                                Float.parseFloat(map.get("pnlpercentage").toString()));
                holdingList.add(holding);
            }
            if(holdingList.size() > 0) {
                 angelTotalHolding = new AngelTotalHolding(holdingList,Float.parseFloat(totalHoldings.get("totalholdingvalue").toString()),
                        Float.parseFloat(totalHoldings.get("totalprofitandloss").toString()),
                        Float.parseFloat(totalHoldings.get("totalpnlpercentage").toString()),
                        Float.parseFloat(totalHoldings.get("totalinvvalue").toString()));
            }
        } catch (IOException | SmartAPIException e) {
            log.error("Client is empty {}", e.getMessage());
            throw new SmartAPIException("Client is empty or Session Expired!!");
        }
        return angelTotalHolding;
    }

    @Override
    public void loadAllTheInstruments() throws KiteException, IOException {
        List<Instrument> instruments = new ArrayList<>();
        try {
            instruments = tsqcoConfig.getKiteConnect().getInstruments("NSE");
        } catch (Exception ex ){
            log.error("Exception Occurred while getting all Instruments");
        }
        log.info("Today's Instrument Size {}", instruments.size());
        if(!instruments.isEmpty()) {
            List<TsqcoKiteInstruments> instrumentList = new ArrayList<>();
            for (Instrument instrument : instruments) {
                TsqcoKiteInstruments kiteInstruments = new TsqcoKiteInstruments(instrument);
                instrumentList.add(kiteInstruments);
                if (instrumentList.size() >= TsqcoConstants.BATCH_SIZE) {
                    tsqcoKiteInstrumentsRepo.saveAll(instrumentList);
                }
            }
            tsqcoKiteInstrumentsRepo.saveAll(instrumentList);
        }
    }


    @Override
    public String loadAllAngelInstruments() throws InterruptedException {
        /*tsqcoAngelInstrumentsRepo.truncateInstruments();
        tsqcoAngelInstrumentsRepo.resetInstrumentSequence();*/
        //tsqcoAngelInstrumentsRepo.manageInstrumentsTable();
        long startTime = System.currentTimeMillis();
        Optional<AngelApplicationConfig> instrumentLastLoaded = tsqcoAngelAppConfigRepo.findById(1);
        if (!instrumentLastLoaded.isPresent() || !DateHelper.timeRangeCheck(instrumentLastLoaded.get().getInstrumentlastloaded().toString())) {
            webClient.get()
                    .uri("/OpenAPI_File/files/OpenAPIScripMaster.json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(this::processAndSave)
                    .block();
            AngelApplicationConfig config = new AngelApplicationConfig();
             if(instrumentLastLoaded.isPresent()) {
                 config = instrumentLastLoaded.get();
             } else {
                 config.setInstrumentlastloaded(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString());
             }
            tsqcoAngelAppConfigRepo.save(config);
            return INSTRUMENT_LOADED;
        } else {
            tsqcoAngelAppConfigRepo.save(new AngelApplicationConfig(1, ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString()));
            return INSTRUMENT_NOT_LOADED;
        }
    }

    private Mono<Void> processAndSave(String jsonData) {
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            Set<String> symbolSet = Arrays.stream(tsqcoProps.getFilterSymbol().split(","))
                    .collect(Collectors.toSet());
            List<TsqcoAngelInstruments> instruments = objectMapper.readValue(jsonData, new TypeReference<List<TsqcoAngelInstruments>>(){});
            List<TsqcoAngelInstruments> filteredInstruments = instruments.stream().filter(instrument -> symbolSet.contains(instrument.getExchseg())).collect(Collectors.toList());
            List<TsqcoAngelInstruments> savedInstruments = tsqcoAngelInstrumentsRepo.saveAll(filteredInstruments);
            log.debug("Total No of Instruments loaded to DB {} ", filteredInstruments.size());
            List<TsqcoAngelInstruments> filterByEquityInstruments = savedInstruments.stream().filter(e -> e.getSymbol().endsWith("EQ")).collect(Collectors.toList());
            log.debug("Total No of Instruments filtered with Equatity {} ", filterByEquityInstruments.size());
            for(TsqcoAngelInstruments tai: filterByEquityInstruments ){
                AngelMarketData marketData = getMarketDataWithRetry(tai.getExchseg(), tai.getToken());
                if(null != marketData) {
                    tai.setLtp(marketData.getLtp());
                    tai.setPercentagechange(marketData.getPercentChange());
                    tai.setIntrumentdate(LocalDateTime.now());
                    tsqcoAngelInstrumentsRepo.save(tai);
                }
            }
            log.debug("Total No of Instruments updated with LTP {} ", filterByEquityInstruments.size());
        } catch (Exception e) {
            log.error(String.valueOf(e));
        }
        return Mono.empty();
    }

    public AngelMarketData getMarketDataWithRetry(String exch, String token) throws InterruptedException {
        int attempts = 0;
        AngelMarketData marketData = null;
        int backoff = 2000;
        while (attempts < MAX_RETRIES) {
            try {
                rateLimiter.acquire();
                marketData = getMarketData(new AngelMarketData(exch, token, "FULL"));
                break; // If the request is successful, break the loop
            } catch (RuntimeException e) {
                attempts++;
                Thread.sleep(backoff);
                backoff *= 2;
                log.error("Attempt " + attempts + " failed: " + e.getMessage() + " Token No :" +token);
                if (attempts >= MAX_RETRIES) {
                    log.error("All attempts failed.");
                }
            }
        }
        return marketData;
    }


    @Override
    public AngelMarketData getMarketData(AngelMarketData mktData) throws RuntimeException{

        try {
            JSONObject payload = new JSONObject();
            payload.put("mode", mktData.getMode());
            JSONObject exchangeTokens = new JSONObject();
            JSONArray nseTokens = new JSONArray();
            nseTokens.put(mktData.getSymbolToken());
            exchangeTokens.put(mktData.getExchange(), nseTokens);
            payload.put("exchangeTokens", exchangeTokens);
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<AngelMarketData>> jacksonTypeReference = new TypeReference<List<AngelMarketData>>() {};
            List<AngelMarketData> marketData = objectMapper.readValue(String.valueOf(tsqcoConfig.getSmartConnect()
                    .marketData(payload).get("fetched")), jacksonTypeReference);
            return marketData.get(0);
        } catch (SmartAPIException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AngelGainersLosers> getTopGainersAndLosers(String targetDate, int topN, boolean avgFlag) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date parsedDate = dateFormat.parse(targetDate);
        Date sqlDate = new Date(parsedDate.getTime());
        if(avgFlag) {
            return tsqcoAngelGainersAndLoserRepo.getTopGainersAndLosersForLastFiveDays(sqlDate, topN);
        } else {
            return tsqcoAngelGainersAndLoserRepo.getTopGainersAndLosers(sqlDate, topN);
        }
    }


}
