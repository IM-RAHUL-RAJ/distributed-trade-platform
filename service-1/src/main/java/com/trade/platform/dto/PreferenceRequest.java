package com.trade.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreferenceRequest(
        @NotBlank String tradingExperience,
        @NotBlank String riskTolerance,
        @NotBlank String tradingStyle,
        @NotBlank String investmentHorizon,
        @NotEmpty List<String> preferredSectors,
        @NotBlank String tradingFrequency) {
}