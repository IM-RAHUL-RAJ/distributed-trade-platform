package com.trade.platform.controller;

import com.trade.platform.dto.MarketDataDto;
import com.trade.platform.dto.TransactionDto;
import com.trade.platform.dto.TradeDto;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.MarketDataService;
import com.trade.platform.service.Service2Client;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DataController {

    private final MarketDataService marketDataService;
    private final Service2Client service2Client;

    @GetMapping("/trades")
    public List<TradeDto> trades(@CurrentUser UUID userId) {
        return service2Client.trades(userId);
    }

    @GetMapping("/transactions")
    public List<TransactionDto> transactions(@CurrentUser UUID userId) {
        return service2Client.transactions(userId);
    }

    @GetMapping("/market-data")
    public List<MarketDataDto> marketData() {
        return marketDataService.liveMarketData();
    }

    @GetMapping("/watchlist")
    public List<MarketDataDto> watchlist(@CurrentUser UUID userId) {
        return marketDataService.watchlist(userId);
    }
}