package com.trade.platform.s2.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class MarketData {
    private UUID instrumentId;
    private String symbol;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Instant timestamp;
}