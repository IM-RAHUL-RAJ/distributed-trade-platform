package com.trade.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Transaction {
    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID orderId;
    private UUID tradeId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Instant createdAt;
}