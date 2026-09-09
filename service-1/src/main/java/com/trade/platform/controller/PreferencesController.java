package com.trade.platform.controller;

import com.trade.platform.dto.PreferenceRequest;
import com.trade.platform.dto.PreferenceResponse;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.PreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferenceService preferenceService;

    @GetMapping
    public PreferenceResponse get(@CurrentUser UUID userId) {
        return preferenceService.get(userId);
    }

    @PostMapping
    public PreferenceResponse save(@CurrentUser UUID userId, @Valid @RequestBody PreferenceRequest request) {
        return preferenceService.save(userId, request);
    }

    @PutMapping
    public PreferenceResponse update(@CurrentUser UUID userId, @Valid @RequestBody PreferenceRequest request) {
        return preferenceService.save(userId, request);
    }
}