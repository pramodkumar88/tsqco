package com.tsqco.models.dto;

import lombok.Data;

@Data
public class AngelCandleStickResponseDTO {
    private String datetime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
}
