package com.trade.platform.s2.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Trade {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private UUID accountId;
    private UUID instrumentId;
    private String symbol;
    private String side;
    private Integer quantity;
    private BigDecimal price;
    private Instant executedAt;
}