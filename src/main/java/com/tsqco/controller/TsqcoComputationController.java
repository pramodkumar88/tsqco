package com.tsqco.controller;


import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.tsqco.constants.TsqcoConstants;
import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.AngelCandleStickRequestDTO;
import com.tsqco.models.dto.AngelCandleStickResponseDTO;
import com.tsqco.service.TsqcoComputationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/tsqco")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = TsqcoConstants.LOCALHOST_WEB)
public class TsqcoComputationController {

    private final TsqcoComputationService computationService;


    @PostMapping(value = "/angel/candlestickdata", produces = "application/json")
    public List<AngelCandleStickResponseDTO> getCandleStickData(@RequestBody AngelCandleStickRequestDTO candleStickRequest) throws SmartAPIException, IOException {
        return computationService.getCandleStickData(candleStickRequest);
    }

    @GetMapping(value = "/angel/search", produces = "application/json")
    public  List<TsqcoAngelInstruments> searchResult(@RequestParam String tradingsymbol,
                                                     @RequestParam(defaultValue = "ALL") String exsegment) {
        return computationService.getSearchResults(tradingsymbol.toUpperCase(), exsegment.toUpperCase());
    }
}
