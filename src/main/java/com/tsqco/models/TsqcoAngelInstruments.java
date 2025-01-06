package com.tsqco.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "tsqco_angel_instruments", schema = "tsqco")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TsqcoAngelInstruments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long instrument_id;
    private String token;
    private String symbol;
    private String name;
    private String expiry;
    private String strike;
    private String lotsize;
    private String instrumenttype;
    @JsonProperty("exch_seg")
    @Column(name = "exch_seg", nullable = false)
    private String exchseg;
    private String tick_size;
    private Float percentagechange;
    private Float ltp;
    private LocalDateTime intrumentdate;
    private Float eps;
}
