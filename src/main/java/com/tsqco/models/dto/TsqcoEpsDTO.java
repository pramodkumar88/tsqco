package com.tsqco.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TsqcoEpsDTO  {
    private Long instrument_id;
    private String name;
}
