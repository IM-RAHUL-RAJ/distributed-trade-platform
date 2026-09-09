package com.trade.platform.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        AccountDto account,
        PortfolioSummary portfolio,
        List<PositionDto> holdings,
        List<PositionDto> positions,
        List<MarketDataDto> watchlist,
        List<MarketDataDto> marketData,
        List<OrderResponse> openOrders,
        List<TransactionDto> recentTransactions,
        List<TradeDto> recentTrades) {

    public record PortfolioSummary(
            BigDecimal availableCash,
            BigDecimal invested,
            BigDecimal totalValue,
            BigDecimal dayChange,
            BigDecimal dayChangePercent,
            BigDecimal totalPnl) {
    }
}