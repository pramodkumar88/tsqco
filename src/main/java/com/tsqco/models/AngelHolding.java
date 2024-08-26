package com.tsqco.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AngelHolding {
    private String tradingsymbol;
    private String exchange;
    private String isin;
    private int t1quantity;
    private int realisedquantity;
    private int quantity;
    private int authorisedquantity;
    private String product;
    private String collateralquantity;
    private String collateraltype;
    private int haircut;
    private float averageprice;
    private float ltp;
    private int symboltoken;
    private float close;
    private float profitandloss;
    private float pnlpercentage;
}



