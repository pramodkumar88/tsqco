package com.tsqco.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AngelTotalHolding {
    private List<AngelHolding> listHoldings;
    private float totalholdingvalue;
    private float totalprofitandloss;
    private float totalpnlpercentage;
    private float totalinvvalue;
}

