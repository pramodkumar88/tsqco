package com.tsqco.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TsqcoInstrumentByEquityDTO {
    private Long instrument_id;
    private String name;
    private String token;
    private String symbol;
    @JsonProperty("exch_seg")
    @Column(name = "exch_seg", nullable = false)
    private String exchseg;
    private Float ltp;
}
