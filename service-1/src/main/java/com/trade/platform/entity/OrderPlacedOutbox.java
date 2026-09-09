package com.trade.platform.entity;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OrderPlacedOutbox {
    private UUID id;
    private UUID orderId;
    private UUID eventId;
    private String payload;
    private String status;
    private Instant publishedAt;
    private Instant createdAt;
}