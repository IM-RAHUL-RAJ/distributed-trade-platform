package com.trade.platform.controller;

import com.trade.platform.dto.PositionDto;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/portfolio")
    public List<PositionDto> portfolio(@CurrentUser UUID userId) {
        return portfolioService.holdings(userId);
    }

    @GetMapping("/positions")
    public List<PositionDto> positions(@CurrentUser UUID userId) {
        return portfolioService.positions(userId);
    }
}