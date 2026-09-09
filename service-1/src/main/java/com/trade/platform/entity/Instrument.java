package com.trade.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Instrument {
    private UUID id;
    private String symbol;
    private String name;
    private String exchange;
    private String currency;
    private String type;
    private BigDecimal tickSize;
    private Integer lotSize;
    private BigDecimal lastPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Instant updatedAt;
    private Boolean isActive;
}