package com.trade.platform.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionDto(
        UUID id,
        UUID instrumentId,
        String symbol,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal realizedPnl,
        BigDecimal lastPrice,
        BigDecimal currentValue,
        BigDecimal pnl,
        BigDecimal pnlPercent) {
}