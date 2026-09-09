package com.trade.platform.entity;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CustomerPreference {
    private UUID id;
    private UUID userId;
    private String tradingExperience;
    private String riskTolerance;
    private String tradingStyle;
    private String investmentHorizon;
    private String preferredSectors;
    private String tradingFrequency;
    private Boolean isCompleted;
    private Instant createdAt;
    private Instant updatedAt;
}