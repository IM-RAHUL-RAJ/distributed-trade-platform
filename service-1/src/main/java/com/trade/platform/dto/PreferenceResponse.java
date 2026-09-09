package com.trade.platform.dto;

import java.util.List;

public record PreferenceResponse(
        boolean completed,
        String tradingExperience,
        String riskTolerance,
        String tradingStyle,
        String investmentHorizon,
        List<String> preferredSectors,
        String tradingFrequency) {
}