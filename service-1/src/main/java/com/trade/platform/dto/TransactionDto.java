package com.trade.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
        UUID id,
        UUID orderId,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt) {
}