package com.trade.platform.s2.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        UUID instrumentId,
        String symbol,
        String side,
        Integer quantity,
        BigDecimal requestedPrice,
        BigDecimal executedPrice,
        String status,
        String rejectReason,
        Instant timestamp) {
}