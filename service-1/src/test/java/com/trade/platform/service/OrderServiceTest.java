package com.trade.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.common.BusinessException;
import com.trade.platform.dto.OrderRequest;
import com.trade.platform.dto.OrderResponse;
import com.trade.platform.entity.Account;
import com.trade.platform.entity.Instrument;
import com.trade.platform.mapper.OrderMapper;
import com.trade.platform.mapper.OutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OutboxMapper outboxMapper;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private AccountService accountService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private Instrument instrument(String symbol) {
        Instrument i = new Instrument();
        i.setId(UUID.randomUUID());
        i.setSymbol(symbol);
        i.setLotSize(1);
        i.setIsActive(true);
        return i;
    }

    private Account account() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setCash(new BigDecimal("1000000"));
        return a;
    }

    @Test
    void createsOrderAndEnqueuesOrderPlacedEventInOutbox() throws Exception {
        UUID userId = UUID.randomUUID();
        Instrument instrument = instrument("NVDA");
        Account account = account();
        when(instrumentService.findBySymbol("NVDA")).thenReturn(Optional.of(instrument));
        when(accountService.getOrCreate(userId)).thenReturn(account);
        when(orderMapper.insert(any())).thenReturn(1);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OrderResponse response = orderService.create(userId,
                new OrderRequest("NVDA", "BUY", "MARKET", 10, null));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.side()).isEqualTo("BUY");
        assertThat(response.quantity()).isEqualTo(10);

        ArgumentCaptor<com.trade.platform.entity.OrderPlacedOutbox> outboxCaptor =
                ArgumentCaptor.forClass(com.trade.platform.entity.OrderPlacedOutbox.class);
        verify(outboxMapper).insert(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(outboxCaptor.getValue().getPayload()).isEqualTo("{}");
        verify(orderMapper).insert(any());
    }

    @Test
    void rejectsUnknownInstrument() {
        when(instrumentService.findBySymbol("???")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(),
                new OrderRequest("???", "BUY", "MARKET", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unknown instrument");
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    void rejectsInvalidSide() {
        Instrument i = instrument("NVDA");
        when(instrumentService.findBySymbol("NVDA")).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(),
                new OrderRequest("NVDA", "HOLD", "MARKET", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("side must be BUY or SELL");
    }

    @Test
    void rejectsQuantityNotMultipleOfLotSize() {
        Instrument i = instrument("NVDA");
        i.setLotSize(5);
        when(instrumentService.findBySymbol("NVDA")).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(),
                new OrderRequest("NVDA", "BUY", "MARKET", 3, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("multiple of lot size");
    }

    @Test
    void rejectsLimitOrderWithoutPrice() {
        Instrument i = instrument("NVDA");
        when(instrumentService.findBySymbol("NVDA")).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(),
                new OrderRequest("NVDA", "BUY", "LIMIT", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requestedPrice is required");
    }

    @Test
    void cancelsOnlyPendingOrders() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(orderMapper.findByIdAndUser(orderId, userId)).thenReturn(null);
        assertThatThrownBy(() -> orderService.cancel(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order not found");
    }
}