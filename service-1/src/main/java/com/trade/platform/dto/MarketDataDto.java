package com.trade.platform.dto;

import java.math.BigDecimal;

public record MarketDataDto(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent) {
}