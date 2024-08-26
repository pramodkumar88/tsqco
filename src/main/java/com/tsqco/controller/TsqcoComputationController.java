package com.tsqco.controller;


import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.config.TsqcoProperties;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.TsqcoInstrumentDTO;
import com.tsqco.service.TsqcoComputationService;
import com.tsqco.service.TsqcoDashBoardService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/tsqco")
@Slf4j
@AllArgsConstructor
public class TsqcoComputationController {

    private final TsqcoComputationService computationService;

    private final TsqcoProperties tsqcoProps;

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @GetMapping(value = "/angel/computation", produces = "application/json")
    public void getCompute(@RequestParam String tradingSymbol,
                           @RequestParam("fromdate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date fromDate,
                           @RequestParam("todate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date toDate) throws SmartAPIException, IOException {
        computationService.getCompute(tradingSymbol, fromDate, toDate);
    }

    @CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
    @GetMapping(value = "/angel/search", produces = "application/json")
    public  List<TsqcoAngelInstruments> searchResult(@RequestParam String tradingsymbol,
                                                     @RequestParam(defaultValue = "ALL") String exsegment) {
        return computationService.getSearchResults(tradingsymbol.toUpperCase(), exsegment.toUpperCase());
    }



}
