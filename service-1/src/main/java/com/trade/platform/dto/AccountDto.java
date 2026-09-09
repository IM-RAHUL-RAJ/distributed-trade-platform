package com.trade.platform.dto;

import java.math.BigDecimal;

public record AccountDto(
        String symbol,
        String name,
        BigDecimal cash,
        BigDecimal margin) {
}