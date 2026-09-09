package com.trade.platform.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketDataEvent(
        UUID eventId,
        UUID instrumentId,
        String symbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent,
        Instant timestamp) {
}