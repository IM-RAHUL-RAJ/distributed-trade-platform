package com.trade.platform.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InstrumentDto(
        UUID id,
        String symbol,
        String name,
        String exchange,
        String currency,
        String type,
        BigDecimal lastPrice,
        BigDecimal change,
        BigDecimal changePercent) {
}