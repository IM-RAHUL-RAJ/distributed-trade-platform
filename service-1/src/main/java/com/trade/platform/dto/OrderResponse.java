package com.trade.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID instrumentId,
        String symbol,
        String side,
        String orderType,
        Integer quantity,
        BigDecimal requestedPrice,
        BigDecimal executedPrice,
        String status,
        String rejectReason,
        Instant createdAt) {
}