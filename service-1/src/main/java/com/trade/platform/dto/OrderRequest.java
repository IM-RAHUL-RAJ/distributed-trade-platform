package com.trade.platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String symbol,
        @NotBlank String side,
        @NotBlank String orderType,
        @NotNull @Min(1) Integer quantity,
        BigDecimal requestedPrice) {
}