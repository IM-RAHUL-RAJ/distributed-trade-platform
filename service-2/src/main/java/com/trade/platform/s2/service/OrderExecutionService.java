package com.trade.platform.s2.service;

import com.trade.platform.s2.entity.Account;
import com.trade.platform.s2.entity.OrderRow;
import com.trade.platform.s2.entity.Position;
import com.trade.platform.s2.entity.Trade;
import com.trade.platform.s2.entity.Transaction;
import com.trade.platform.s2.event.OrderPlacedEvent;
import com.trade.platform.s2.mapper.AccountMapper;
import com.trade.platform.s2.mapper.MarketDataMapper;
import com.trade.platform.s2.mapper.OrderMapper;
import com.trade.platform.s2.mapper.PositionMapper;
import com.trade.platform.s2.mapper.ProcessedEventMapper;
import com.trade.platform.s2.mapper.TradeMapper;
import com.trade.platform.s2.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Core financially-sensitive order execution. Every code path that mutates
 * cash, positions, orders, trades or transactions runs inside a single DB
 * transaction that rolls back entirely on any failure.
 *
 * Idempotency: processed_events (eventId) + unique trade per orderId prevent
 * duplicate executions.
 */
@Slf4j
@Service
public class OrderExecutionService {

    private final AccountMapper accountMapper;
    private final OrderMapper orderMapper;
    private final PositionMapper positionMapper;
    private final TradeMapper tradeMapper;
    private final TransactionMapper transactionMapper;
    private final ProcessedEventMapper processedEventMapper;
    private final MarketDataMapper marketDataMapper;

    public OrderExecutionService(AccountMapper accountMapper, OrderMapper orderMapper,
                                 PositionMapper positionMapper, TradeMapper tradeMapper,
                                 TransactionMapper transactionMapper,
                                 ProcessedEventMapper processedEventMapper,
                                 MarketDataMapper marketDataMapper) {
        this.accountMapper = accountMapper;
        this.orderMapper = orderMapper;
        this.positionMapper = positionMapper;
        this.tradeMapper = tradeMapper;
        this.transactionMapper = transactionMapper;
        this.processedEventMapper = processedEventMapper;
        this.marketDataMapper = marketDataMapper;
    }

    /**
     * @return the execution outcome so the consumer can publish the proper
     *         trade-event (EXECUTED / REJECTED) after commit.
     */
    @Transactional
    public ExecutionResult execute(OrderPlacedEvent event, String rawPayload) {
        int inserted = processedEventMapper.insertIgnore(event.eventId().toString(), "order-placed",
                event.orderId(), rawPayload);
        if (inserted == 0) {
            log.info("Duplicate order-placed event {} ignored", event.eventId());
            return ExecutionResult.duplicate();
        }
        if (tradeMapper.findByOrderId(event.orderId()) != null) {
            log.warn("Order {} already traded; ignoring duplicate execution", event.orderId());
            return ExecutionResult.duplicate();
        }

        OrderRow order = orderMapper.findById(event.orderId());
        if (order == null) {
            log.warn("Order {} not found; skipping", event.orderId());
            return ExecutionResult.skip("ORDER_NOT_FOUND");
        }
        if (!"PENDING".equals(order.getStatus())) {
            log.info("Order {} already in status {}; skipping", order.getId(), order.getStatus());
            return ExecutionResult.skip(order.getStatus());
        }

        Account account = accountMapper.lockById(order.getAccountId());
        if (account == null) {
            return reject(order, "Account not found");
        }

        BigDecimal price = resolveExecutionPrice(order);
        if (price == null) {
            return reject(order, "No market price available for " + order.getSymbol());
        }

        if ("LIMIT".equals(order.getOrderType())) {
            boolean marketable = "BUY".equals(order.getSide())
                    ? price.compareTo(order.getRequestedPrice()) <= 0
                    : price.compareTo(order.getRequestedPrice()) >= 0;
            if (!marketable) {
                return reject(order, "Limit not met (price " + price + " vs limit " + order.getRequestedPrice() + ")");
            }
        }

        BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
        BigDecimal notional = price.multiply(quantity);

        Position position = null;
        if ("SELL".equals(order.getSide())) {
            position = positionMapper.lockByUserAndInstrument(order.getUserId(), order.getInstrumentId());
            if (position == null || position.getQuantity() < order.getQuantity()) {
                return reject(order, "Insufficient holdings to sell " + order.getQuantity() + " " + order.getSymbol());
            }
        } else if (account.getCash().compareTo(notional) < 0) {
            return reject(order, "Insufficient cash to buy " + order.getQuantity() + " " + order.getSymbol()
                    + " @ " + price + " (need " + notional + ", have " + account.getCash() + ")");
        }

        ExecutedTradeInformation info = applyTrade(order, account, position, price, notional, quantity);
        log.info("Executed order {} side={} qty={} symbol={} @ {} status=EXECUTED",
                order.getId(), order.getSide(), order.getQuantity(), order.getSymbol(), price);
        return ExecutionResult.executed(order.getId(), price, info.tradeId() != null);
    }

    private ExecutedTradeInformation applyTrade(OrderRow order, Account account, Position position,
                                                BigDecimal price, BigDecimal notional, BigDecimal quantity) {
        BigDecimal newCash;
        if ("BUY".equals(order.getSide())) {
            newCash = account.getCash().subtract(notional);
        } else {
            newCash = account.getCash().add(notional);
        }
        accountMapper.updateCash(account.getId(), newCash);

        orderMapper.updateStatus(order.getId(), "EXECUTED", price, null);

        Trade trade = new Trade();
        trade.setId(UUID.randomUUID());
        trade.setOrderId(order.getId());
        trade.setUserId(order.getUserId());
        trade.setAccountId(order.getAccountId());
        trade.setInstrumentId(order.getInstrumentId());
        trade.setSymbol(order.getSymbol());
        trade.setSide(order.getSide());
        trade.setQuantity(order.getQuantity());
        trade.setPrice(price);
        tradeMapper.insert(trade);

        if ("BUY".equals(order.getSide())) {
            Position current = position != null ? position
                    : positionMapper.lockByUserAndInstrument(order.getUserId(), order.getInstrumentId());
            applyBuyPosition(current, order, notional, quantity);
        } else {
            applySellPosition(position, order, price, quantity);
        }

        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID());
        txn.setUserId(order.getUserId());
        txn.setAccountId(order.getAccountId());
        txn.setOrderId(order.getId());
        txn.setTradeId(trade.getId());
        txn.setType("BUY".equals(order.getSide()) ? "TRADE_BUY" : "TRADE_SELL");
        txn.setAmount("BUY".equals(order.getSide()) ? notional.negate() : notional);
        txn.setBalanceAfter(newCash);
        txn.setDescription(order.getSide() + " " + order.getQuantity() + " " + order.getSymbol()
                + " @ " + price);
        transactionMapper.insert(txn);

        return new ExecutedTradeInformation(trade.getId());
    }

    private void applyBuyPosition(Position current, OrderRow order, BigDecimal notional, BigDecimal quantity) {
        if (current == null) {
            Position p = new Position();
            p.setId(UUID.randomUUID());
            p.setUserId(order.getUserId());
            p.setAccountId(order.getAccountId());
            p.setInstrumentId(order.getInstrumentId());
            p.setSymbol(order.getSymbol());
            p.setQuantity(order.getQuantity());
            p.setAveragePrice(notional.divide(quantity, 4, RoundingMode.HALF_UP));
            p.setRealizedPnl(BigDecimal.ZERO);
            positionMapper.insert(p);
        } else {
            BigDecimal oldQty = BigDecimal.valueOf(current.getQuantity());
            BigDecimal totalQty = oldQty.add(quantity);
            BigDecimal avg = current.getAveragePrice().multiply(oldQty).add(notional)
                    .divide(totalQty, 4, RoundingMode.HALF_UP);
            current.setQuantity(current.getQuantity() + order.getQuantity());
            current.setAveragePrice(avg);
            positionMapper.update(current);
        }
    }

    private void applySellPosition(Position position, OrderRow order, BigDecimal price, BigDecimal quantity) {
        BigDecimal avg = position.getAveragePrice();
        BigDecimal realized = price.subtract(avg).multiply(quantity);
        position.setQuantity(position.getQuantity() - order.getQuantity());
        position.setRealizedPnl(position.getRealizedPnl().add(realized));
        if (position.getQuantity() == 0) {
            position.setAveragePrice(BigDecimal.ZERO);
        }
        positionMapper.update(position);
    }

    private ExecutionResult reject(OrderRow order, String reason) {
        orderMapper.updateStatus(order.getId(), "REJECTED", null, reason);
        log.warn("Order {} REJECTED: {}", order.getId(), reason);
        return ExecutionResult.rejected(order.getId(), reason);
    }

    /** Execution price priority: live market_data row, then instrument.last_price. */
    private BigDecimal resolveExecutionPrice(OrderRow order) {
        var md = marketDataMapper.findLatest(order.getInstrumentId());
        if (md != null) {
            return md.getPrice();
        }
        var instrument = marketDataMapper.findInstrument(order.getInstrumentId());
        return instrument != null ? instrument.getLastPrice() : null;
    }

    public record ExecutionResult(String outcome, UUID orderId, BigDecimal executedPrice, String rejectReason) {
        public static ExecutionResult duplicate() {
            return new ExecutionResult("DUPLICATE", null, null, null);
        }

        public static ExecutionResult skip(String state) {
            return new ExecutionResult("SKIPPED", null, null, state);
        }

        public static ExecutionResult executed(UUID orderId, BigDecimal price, boolean traded) {
            return new ExecutionResult("EXECUTED", orderId, price, null);
        }

        public static ExecutionResult rejected(UUID orderId, String reason) {
            return new ExecutionResult("REJECTED", orderId, null, reason);
        }

        public boolean shouldPublish() {
            return "EXECUTED".equals(outcome) || "REJECTED".equals(outcome);
        }
    }

    private record ExecutedTradeInformation(UUID tradeId) {
    }
}