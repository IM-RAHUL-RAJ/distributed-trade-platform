package com.trade.platform.s2.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Account {
    private UUID id;
    private UUID userId;
    private BigDecimal cash;
    private BigDecimal margin;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}