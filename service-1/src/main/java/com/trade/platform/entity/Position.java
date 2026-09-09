package com.trade.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Position {
    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID instrumentId;
    private String symbol;
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal realizedPnl;
    private Instant updatedAt;
}