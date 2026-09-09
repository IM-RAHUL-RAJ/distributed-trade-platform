package com.trade.platform.s2.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTOs exposed by Service 2's internal read API (/api/internal). Service 1
 * consumes these over HTTP to render the dashboard without sharing a database.
 */
public final class InternalDtos {

    private InternalDtos() {
    }

    public record AccountDto(String symbol, String name, BigDecimal cash, BigDecimal margin) {
    }

    public record PositionDto(UUID id, UUID instrumentId, String symbol, Integer quantity,
                              BigDecimal averagePrice, BigDecimal realizedPnl) {
    }

    public record TradeDto(UUID id, UUID orderId, String symbol, String side, Integer quantity,
                           BigDecimal price, Instant executedAt) {
    }

    public record TransactionDto(UUID id, UUID orderId, String type, BigDecimal amount,
                                 BigDecimal balanceAfter, String description, Instant createdAt) {
    }
}