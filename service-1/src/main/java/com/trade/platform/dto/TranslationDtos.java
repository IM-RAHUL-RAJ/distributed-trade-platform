package com.trade.platform.dto;

import com.trade.platform.entity.Instrument;
import com.trade.platform.entity.OrderRow;
import com.trade.platform.entity.Trade;
import com.trade.platform.entity.Transaction;

import java.math.BigDecimal;

public final class TranslationDtos {

    private TranslationDtos() {
    }

    public static MarketDataDto fromInstrument(Instrument i) {
        return new MarketDataDto(i.getSymbol(), i.getName(), i.getLastPrice(),
                i.getChange() != null ? i.getChange() : BigDecimal.ZERO,
                i.getChangePercent() != null ? i.getChangePercent() : BigDecimal.ZERO);
    }

    public static OrderResponse toOrderResponse(OrderRow o) {
        return new OrderResponse(o.getId(), o.getInstrumentId(), o.getSymbol(), o.getSide(),
                o.getOrderType(), o.getQuantity(), o.getRequestedPrice(), o.getExecutedPrice(),
                o.getStatus(), o.getRejectReason(), o.getCreatedAt());
    }

    public static TransactionDto toTransactionDto(Transaction t) {
        return new TransactionDto(t.getId(), t.getOrderId(), t.getType(), t.getAmount(),
                t.getBalanceAfter(), t.getDescription(), t.getCreatedAt());
    }

    public static TradeDto toTradeDto(Trade t) {
        return new TradeDto(t.getId(), t.getOrderId(), t.getSymbol(), t.getSide(),
                t.getQuantity(), t.getPrice(), t.getExecutedAt());
    }
}