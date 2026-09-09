package com.trade.platform.controller;

import com.trade.platform.dto.DashboardResponse;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse get(@CurrentUser UUID userId) {
        return dashboardService.build(userId);
    }
}