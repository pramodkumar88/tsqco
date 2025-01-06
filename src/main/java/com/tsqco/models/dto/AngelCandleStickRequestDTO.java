package com.tsqco.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AngelCandleStickRequestDTO {
    private String token;
    private String exchange;
    private String symbol;
    private String interval;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
