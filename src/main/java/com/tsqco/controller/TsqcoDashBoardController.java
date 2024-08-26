package com.tsqco.controller;

import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.models.AngelGainersLosers;
import com.tsqco.models.AngelMarketData;
import com.tsqco.models.AngelTotalHolding;
import com.tsqco.models.AngelMarketData;
import com.tsqco.service.TsqcoDashBoardService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/tsqco")
@AllArgsConstructor
public class TsqcoDashBoardController {

    private final TsqcoDashBoardService tsqcoDashBoardService;

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @GetMapping(value = "/portfolio", produces = "application/json")
    public ResponseEntity<List<Holding>> getKitePortfolio() throws KiteException {
        List<Holding> userPortfolio = tsqcoDashBoardService.getKitePortfolio();
        return new ResponseEntity<>(userPortfolio, HttpStatus.OK);
    }

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @GetMapping(value = "/angel/portfolio", produces = "application/json")
    public ResponseEntity<AngelTotalHolding> getAngelPortfolio() throws SmartAPIException {
        AngelTotalHolding angelPortfolio = tsqcoDashBoardService.getAngelPortfolio();
        return new ResponseEntity<>(angelPortfolio, HttpStatus.OK);
    }

    @GetMapping(value="/allinstruments")
    public void getAllInstrumentsLoaded() throws KiteException, IOException {
        tsqcoDashBoardService.loadAllTheInstruments();
    }

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @GetMapping(value="/angel/allinstruments")
    public ResponseEntity<String>  getAllAngelInstrumentsLoaded() throws SmartAPIException, InterruptedException {
        String status = tsqcoDashBoardService.loadAllAngelInstruments();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @PostMapping(value="/angel/marketdata")
    public ResponseEntity<AngelMarketData>  getMarketData(@RequestBody AngelMarketData marketdata) throws SmartAPIException {
        AngelMarketData marketData = tsqcoDashBoardService.getMarketData(marketdata);
        return new ResponseEntity<>(marketData, HttpStatus.OK);
    }

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @PostMapping(value="/angel/gainers-losers")
    public ResponseEntity<List<AngelGainersLosers>> getGainersAndLosers(@RequestBody Map<String, Object> request) throws ParseException {
        String targetDate = (String) request.get("target_date");
        int topN = (int) request.get("top_n");
        boolean avgFlag = (boolean) request.get("avg");
        List<AngelGainersLosers> result = tsqcoDashBoardService.getTopGainersAndLosers(targetDate, topN, avgFlag);
        return ResponseEntity.ok(result);
    }


}
