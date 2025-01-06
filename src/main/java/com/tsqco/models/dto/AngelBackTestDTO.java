package com.tsqco.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AngelBackTestDTO {
    private String exchange;
    private String token;
    private String symbol;
    private String interval;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String strategy;
}
