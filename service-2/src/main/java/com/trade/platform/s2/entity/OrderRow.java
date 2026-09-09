package com.trade.platform.s2.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class OrderRow {
    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID instrumentId;
    private String symbol;
    private String side;
    private String orderType;
    private Integer quantity;
    private BigDecimal requestedPrice;
    private BigDecimal executedPrice;
    private String status;
    private String rejectReason;
    private Instant createdAt;
    private Instant updatedAt;
}