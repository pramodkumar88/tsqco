package com.tsqco.models;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tsqco_angel_stock_transactions", schema = "tsqco")
@Data
@Builder
public class TsqcoAngelStockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, length = 15)
    private String token;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "symbol", length = 50)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(name = "purchase_time")
    private LocalDateTime purchaseTime;

    @Column(name = "sell_time")
    private LocalDateTime sellTime;

    @Column(name = "purchase_price")
    private Float purchasePrice;

    @Column(name = "sell_price")
    private Float sellPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;
}
