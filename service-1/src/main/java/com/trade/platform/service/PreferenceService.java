package com.trade.platform.service;

import com.trade.platform.common.BusinessException;
import com.trade.platform.dto.PreferenceRequest;
import com.trade.platform.dto.PreferenceResponse;
import com.trade.platform.entity.CustomerPreference;
import com.trade.platform.mapper.PreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceMapper preferenceMapper;

    @Transactional
    public CustomerPreference ensure(UUID userId) {
        CustomerPreference existing = preferenceMapper.findByUserId(userId);
        if (existing != null) {
            return existing;
        }
        CustomerPreference blank = new CustomerPreference();
        blank.setUserId(userId);
        blank.setIsCompleted(false);
        preferenceMapper.insert(blank);
        log.info("Initialized preferences for user {}", userId);
        return blank;
    }

    @Transactional
    public PreferenceResponse get(UUID userId) {
        return toResponse(ensure(userId));
    }

    @Transactional
    public PreferenceResponse save(UUID userId, PreferenceRequest request) {
        ensure(userId);
        CustomerPreference p = new CustomerPreference();
        p.setUserId(userId);
        p.setTradingExperience(request.tradingExperience());
        p.setRiskTolerance(request.riskTolerance());
        p.setTradingStyle(request.tradingStyle());
        p.setInvestmentHorizon(request.investmentHorizon());
        p.setPreferredSectors(String.join(",", request.preferredSectors()));
        p.setTradingFrequency(request.tradingFrequency());
        preferenceMapper.update(p);
        log.info("Customer preferences saved for user {}", userId);
        return toResponse(preferenceMapper.findByUserId(userId));
    }

    private PreferenceResponse toResponse(CustomerPreference p) {
        if (p == null) {
            return new PreferenceResponse(false, null, null, null, null, List.of(), null);
        }
        List<String> sectors = p.getPreferredSectors() == null || p.getPreferredSectors().isBlank()
                ? List.of()
                : Arrays.stream(p.getPreferredSectors().split(",")).map(String::trim).collect(toList());
        return new PreferenceResponse(
                Boolean.TRUE.equals(p.getIsCompleted()),
                p.getTradingExperience(),
                p.getRiskTolerance(),
                p.getTradingStyle(),
                p.getInvestmentHorizon(),
                sectors,
                p.getTradingFrequency());
    }
}