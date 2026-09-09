package com.trade.platform.s2.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        UUID accountId,
        UUID instrumentId,
        String symbol,
        String side,
        String orderType,
        BigDecimal requestedPrice,
        Integer quantity,
        Instant timestamp) {
}