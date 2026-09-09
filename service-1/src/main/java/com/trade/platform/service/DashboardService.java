package com.trade.platform.service;

import com.trade.platform.dto.DashboardResponse;
import com.trade.platform.dto.TranslationDtos;
import com.trade.platform.mapper.OrderMapper;
import com.trade.platform.mapper.TransactionMapper;
import com.trade.platform.mapper.TradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountService accountService;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final OrderMapper orderMapper;
    private final TransactionMapper transactionMapper;
    private final TradeMapper tradeMapper;

    public DashboardResponse build(UUID userId) {
        accountService.getOrCreate(userId);

        var account = accountService.toDto(userId);
        var portfolio = portfolioService.summary(userId);
        var holdings = portfolioService.holdings(userId);
        var positions = portfolioService.positions(userId);
        var watchlist = marketDataService.watchlist(userId);
        var marketData = marketDataService.liveMarketData();
        var openOrders = orderMapper.findOpenByUser(userId).stream().map(TranslationDtos::toOrderResponse).toList();
        var recentTransactions = transactionMapper.findByUser(userId).stream()
                .limit(8).map(TranslationDtos::toTransactionDto).toList();
        var recentTrades = tradeMapper.findByUser(userId).stream()
                .limit(8).map(TranslationDtos::toTradeDto).toList();

        return new DashboardResponse(account, portfolio, holdings, positions,
                watchlist, marketData, openOrders, recentTransactions, recentTrades);
    }
}