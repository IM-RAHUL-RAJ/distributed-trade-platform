package com.trade.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeDto(
        UUID id,
        UUID orderId,
        String symbol,
        String side,
        Integer quantity,
        BigDecimal price,
        Instant executedAt) {
}