package com.trade.platform.s2.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Instrument {
    private UUID id;
    private String symbol;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
}