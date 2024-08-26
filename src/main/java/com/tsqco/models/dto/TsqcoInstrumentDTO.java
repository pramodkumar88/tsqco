package com.tsqco.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TsqcoInstrumentDTO {
    private String token;
    private String symbol;
    @JsonProperty("exch_seg")
    @Column(name = "exch_seg", nullable = false)
    private String exchseg;
}
