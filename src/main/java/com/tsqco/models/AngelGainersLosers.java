package com.tsqco.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AngelGainersLosers {
    @Id
    @Column(name = "gainer_symbol")
    private String gainerSymbol;
    @Column(name = "gainer_token")
    private String gainerToken;
    @Column(name = "gainer_ltp")
    private Float gainerAvgLtp;
    @Column(name = "gainer_percentagechange")
    private Float gainerAvgPercentageChange;
    @Column(name = "gainer_lotsize")
    private String gainerLotsize;
    @Column(name = "loser_symbol")
    private String loserSymbol;
    @Column(name = "loser_token")
    private String loserToken;
    @Column(name = "loser_ltp")
    private Float loserAvgLtp;
    @Column(name = "loser_percentagechange")
    private Float loserAvgPercentageChange;
    @Column(name = "loser_lotsize")
    private String loserLotsize;
}
