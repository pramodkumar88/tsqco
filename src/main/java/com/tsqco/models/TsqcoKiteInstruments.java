package com.tsqco.models;


import com.zerodhatech.models.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "tsqco_kite_instruments", schema = "tsqco")
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TsqcoKiteInstruments {

    @Id
    @Column(name = "instrument_token")
    @Min(value = 0, message = "Instrument Token should not be empty")
    public long instrumenttoken;

    @Column(name = "exchange_token")
    public long exchangetoken;

    @Column(name = "trading_symbol")
    public String tradingsymbol;

    @Column(name = "name")
    public String name;

    @Column(name = "last_price")
    public double lastprice;

    @Column(name = "tick_size")
    public double ticksize;

    @Column(name = "instrument_type")
    public String instrumenttype;

    @Column(name = "segment")
    public String segment;

    @Column(name = "exchange")
    public String exchange;

    @Column(name = "strike")
    public String strike;

    @Column(name = "lot_size")
    public int lotsize;

    @Column(name = "expiry")
    public Date expiry;

    public TsqcoKiteInstruments(Instrument instrument){
        setInstrumenttoken(instrument.instrument_token);
        setExchangetoken(instrument.exchange_token);
        setTradingsymbol(instrument.tradingsymbol);
        setName(instrument.name);
        setLastprice(instrument.last_price);
        setTicksize(instrument.tick_size);
        setInstrumenttype(instrument.instrument_type);
        setSegment(instrument.segment);
        setExchange(instrument.exchange);
        setStrike(instrument.strike);
        setLotsize(instrument.lot_size);
        setExpiry(instrument.expiry);
    }

}
