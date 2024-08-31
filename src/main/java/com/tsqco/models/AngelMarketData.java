package com.tsqco.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AngelMarketData {
    private String exchange;
    private String tradingSymbol;
    private String symbolToken;
    private Float ltp;
    private Float open;
    private Float high;
    private Float low;
    private Float close;
    private Integer lastTradeQty;
    @JsonFormat(pattern = "dd-MMM-yyyy HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using= DateDeserializers.TimestampDeserializer.class)
    private Timestamp exchFeedTime;
    @JsonFormat(pattern = "dd-MMM-yyyy HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using= DateDeserializers.TimestampDeserializer.class)
    private Timestamp exchTradeTime;
    private Float netChange;
    private Float percentChange;
    private Float avgPrice;
    private Integer tradeVolume;
    private Double opnInterest;
    private Float lowerCircuit;
    private Float upperCircuit;
    private Integer totBuyQuan;
    private Integer totSellQuan;
    @JsonProperty("52WeekLow")
    private Float fiftyTwoWeekLow;
    @JsonProperty("52WeekHigh")
    private Float fiftyTwoWeekHigh;
    @JsonProperty(defaultValue = "FULL")
    private String mode;
    private Float fiftyTwoWeekLows;
    private Float fiftyTwoWeekHighs;
    private List<String> listOfSymbols;

    public Float getFiftyTwoWeekLows() {
        fiftyTwoWeekLows = this.fiftyTwoWeekLow;
        return fiftyTwoWeekLows;
    }

    public Float getFiftyTwoWeekHighs() {
        fiftyTwoWeekHighs = this.fiftyTwoWeekHigh;
        return fiftyTwoWeekHighs ;
    }

    public AngelMarketData(String exchange, String symbolToken, String mode){
        this.exchange = exchange;
        this.symbolToken = symbolToken;
        this.mode= mode;
    }

    public AngelMarketData(String exchange, List listOfSymbols, String mode){
        this.exchange = exchange;
        this.listOfSymbols = listOfSymbols;
        this.mode= mode;
    }
}
