package com.trade.platform.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Raw position data as served by Service 2's internal API. Service 1 enriches
 * it with live market data to build the full PositionDto for the UI.
 */
public record S2PositionDto(
        UUID id,
        UUID instrumentId,
        String symbol,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal realizedPnl) {
}