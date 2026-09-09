package com.trade.platform.s2.service;

import com.trade.platform.s2.entity.Account;
import com.trade.platform.s2.entity.Instrument;
import com.trade.platform.s2.entity.MarketData;
import com.trade.platform.s2.entity.OrderRow;
import com.trade.platform.s2.entity.Position;
import com.trade.platform.s2.event.OrderPlacedEvent;
import com.trade.platform.s2.mapper.AccountMapper;
import com.trade.platform.s2.mapper.MarketDataMapper;
import com.trade.platform.s2.mapper.OrderMapper;
import com.trade.platform.s2.mapper.PositionMapper;
import com.trade.platform.s2.mapper.ProcessedEventMapper;
import com.trade.platform.s2.mapper.TradeMapper;
import com.trade.platform.s2.mapper.TransactionMapper;
import com.trade.platform.s2.service.OrderExecutionService.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private AccountMapper accountMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PositionMapper positionMapper;
    @Mock
    private TradeMapper tradeMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private ProcessedEventMapper processedEventMapper;
    @Mock
    private MarketDataMapper marketDataMapper;

    private OrderExecutionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID INSTRUMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrderExecutionService(accountMapper, orderMapper, positionMapper, tradeMapper,
                transactionMapper, processedEventMapper, marketDataMapper);
    }

    private OrderRow order(String side, String orderType, int quantity, BigDecimal limit) {
        OrderRow o = new OrderRow();
        o.setId(UUID.randomUUID());
        o.setUserId(USER_ID);
        o.setAccountId(ACCOUNT_ID);
        o.setInstrumentId(INSTRUMENT_ID);
        o.setSymbol("NVDA");
        o.setSide(side);
        o.setOrderType(orderType);
        o.setQuantity(quantity);
        o.setRequestedPrice(limit);
        o.setStatus("PENDING");
        return o;
    }

    private Account account(String cash) {
        Account a = new Account();
        a.setId(ACCOUNT_ID);
        a.setUserId(USER_ID);
        a.setCash(new BigDecimal(cash));
        return a;
    }

    private Position position(int quantity, String avg) {
        Position p = new Position();
        p.setUserId(USER_ID);
        p.setAccountId(ACCOUNT_ID);
        p.setInstrumentId(INSTRUMENT_ID);
        p.setQuantity(quantity);
        p.setAveragePrice(new BigDecimal(avg));
        p.setRealizedPnl(BigDecimal.ZERO);
        return p;
    }

    private MarketData marketData(String price) {
        MarketData md = new MarketData();
        md.setInstrumentId(INSTRUMENT_ID);
        md.setPrice(new BigDecimal(price));
        return md;
    }

    private OrderPlacedEvent event(OrderRow order) {
        return new OrderPlacedEvent(UUID.randomUUID(), order.getId(), USER_ID, ACCOUNT_ID, INSTRUMENT_ID,
                "NVDA", order.getSide(), order.getOrderType(), order.getRequestedPrice(), order.getQuantity(),
                Instant.now());
    }

    @Test
    void executesBuyAtMarketPrice() {
        OrderRow order = order("BUY", "MARKET", 5, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("10000"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(marketData("100"));
        when(positionMapper.lockByUserAndInstrument(USER_ID, INSTRUMENT_ID)).thenReturn(null);

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("EXECUTED");
        assertThat(result.executedPrice()).isEqualByComparingTo("100");
        verify(accountMapper).updateCash(ACCOUNT_ID, new BigDecimal("9500"));
        verify(orderMapper).updateStatus(order.getId(), "EXECUTED", new BigDecimal("100"), null);
        verify(tradeMapper).insert(any());
        verify(positionMapper).insert(any());
        verify(transactionMapper).insert(any());
    }

    @Test
    void executesSellReducingPosition() {
        OrderRow order = order("SELL", "MARKET", 3, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("5000"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(marketData("150"));
        when(positionMapper.lockByUserAndInstrument(USER_ID, INSTRUMENT_ID)).thenReturn(position(10, "120"));

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("EXECUTED");
        verify(accountMapper).updateCash(ACCOUNT_ID, new BigDecimal("5450"));
        verify(positionMapper).update(any());
    }

    @Test
    void rejectsBuyWhenCashIsInsufficient() {
        OrderRow order = order("BUY", "MARKET", 100, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("100"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(marketData("2"));

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("REJECTED");
        assertThat(result.rejectReason()).contains("Insufficient cash");
        verify(orderMapper).updateStatus(order.getId(), "REJECTED", null, result.rejectReason());
        verify(accountMapper, never()).updateCash(eq(ACCOUNT_ID), any());
        verify(tradeMapper, never()).insert(any());
    }

    @Test
    void rejectsSellWhenHoldingsAreInsufficient() {
        OrderRow order = order("SELL", "MARKET", 5, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("10000"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(marketData("100"));
        when(positionMapper.lockByUserAndInstrument(USER_ID, INSTRUMENT_ID)).thenReturn(position(2, "100"));

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("REJECTED");
        assertThat(result.rejectReason()).contains("Insufficient holdings");
        verify(tradeMapper, never()).insert(any());
    }

    @Test
    void rejectsLimitOrderWhenNotMarketable() {
        OrderRow order = order("BUY", "LIMIT", 5, new BigDecimal("90"));
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("10000"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(marketData("100"));

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("REJECTED");
        assertThat(result.rejectReason()).contains("Limit not met");
        verify(tradeMapper, never()).insert(any());
    }

    @Test
    void ignoresDuplicateEventFromProcessedLedger() {
        OrderRow order = order("BUY", "MARKET", 1, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(0);

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("DUPLICATE");
        verify(orderMapper, never()).findById(any());
        verify(tradeMapper, never()).insert(any());
    }

    @Test
    void ignoresEventForAlreadyTradedOrder() {
        OrderRow order = order("BUY", "MARKET", 1, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(new com.trade.platform.s2.entity.Trade());

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("DUPLICATE");
    }

    @Test
    void fallsBackToInstrumentLastPriceWhenNoLiveMarketData() {
        OrderRow order = order("BUY", "MARKET", 2, null);
        when(processedEventMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(tradeMapper.findByOrderId(order.getId())).thenReturn(null);
        when(orderMapper.findById(order.getId())).thenReturn(order);
        when(accountMapper.lockById(ACCOUNT_ID)).thenReturn(account("10000"));
        when(marketDataMapper.findLatest(INSTRUMENT_ID)).thenReturn(null);
        Instrument instrument = new Instrument();
        instrument.setId(INSTRUMENT_ID);
        instrument.setLastPrice(new BigDecimal("77.50"));
        when(marketDataMapper.findInstrument(INSTRUMENT_ID)).thenReturn(instrument);

        ExecutionResult result = service.execute(event(order), "{}");

        assertThat(result.outcome()).isEqualTo("EXECUTED");
        assertThat(result.executedPrice()).isEqualByComparingTo("77.50");
    }
}