package com.trade.platform.service;

import com.trade.platform.common.BusinessException;
import com.trade.platform.dto.OrderRequest;
import com.trade.platform.dto.OrderResponse;
import com.trade.platform.entity.Account;
import com.trade.platform.entity.Instrument;
import com.trade.platform.entity.OrderPlacedOutbox;
import com.trade.platform.entity.OrderRow;
import com.trade.platform.event.OrderPlacedEvent;
import com.trade.platform.mapper.OrderMapper;
import com.trade.platform.mapper.OutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OutboxMapper outboxMapper;
    private final InstrumentService instrumentService;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse create(UUID userId, OrderRequest request) {
        String symbol = request.symbol().toUpperCase();
        Instrument instrument = instrumentService.findBySymbol(symbol)
                .orElseThrow(() -> new BusinessException("Unknown instrument: " + symbol));

        if (!instrument.getIsActive()) {
            throw new BusinessException("Instrument is not tradable: " + symbol);
        }
        if (!request.side().equals("BUY") && !request.side().equals("SELL")) {
            throw new BusinessException("side must be BUY or SELL");
        }
        if (!request.orderType().equals("MARKET") && !request.orderType().equals("LIMIT")) {
            throw new BusinessException("orderType must be MARKET or LIMIT");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("quantity must be positive");
        }
        if (request.quantity() % instrument.getLotSize() != 0) {
            throw new BusinessException("quantity must be a multiple of lot size " + instrument.getLotSize());
        }
        if (request.orderType().equals("LIMIT") && (request.requestedPrice() == null || request.requestedPrice().signum() <= 0)) {
            throw new BusinessException("requestedPrice is required for LIMIT orders");
        }

        Account account = accountService.getOrCreate(userId);

        OrderRow order = new OrderRow();
        order.setId(UUID.randomUUID());
        order.setUserId(userId);
        order.setAccountId(account.getId());
        order.setInstrumentId(instrument.getId());
        order.setSymbol(instrument.getSymbol());
        order.setSide(request.side());
        order.setOrderType(request.orderType());
        order.setQuantity(request.quantity());
        order.setRequestedPrice(request.orderType().equals("LIMIT") ? request.requestedPrice() : null);
        order.setStatus("PENDING");
        orderMapper.insert(order);

        enqueueOrderPlaced(order);
        log.info("Order {} created for user {} side={} qty={} symbol={}",
                order.getId(), userId, order.getSide(), order.getQuantity(), order.getSymbol());
        return toResponse(order);
    }

    public List<OrderResponse> list(UUID userId) {
        return orderMapper.findByUser(userId).stream().map(this::toResponse).toList();
    }

    public OrderResponse get(UUID userId, UUID orderId) {
        OrderRow order = orderMapper.findByIdAndUser(orderId, userId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UUID userId, UUID orderId) {
        OrderRow order = orderMapper.findByIdAndUser(orderId, userId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        if (!order.getStatus().equals("PENDING")) {
            throw new BusinessException(409, "Only PENDING orders can be cancelled");
        }
        orderMapper.updateStatus(orderId, "CANCELLED", null, "Cancelled by user");
        log.info("Order {} cancelled by user {}", orderId, userId);
        return toResponse(orderMapper.findById(orderId));
    }

    /**
     * Transactional outbox: the order-placed event is written in the SAME DB
     * transaction as the order row. A scheduled publisher forwards it to Kafka,
     * so we never lose an event between order insert and publish.
     */
    private void enqueueOrderPlaced(OrderRow order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getUserId(),
                order.getAccountId(),
                order.getInstrumentId(),
                order.getSymbol(),
                order.getSide(),
                order.getOrderType(),
                order.getRequestedPrice(),
                order.getQuantity(),
                Instant.now());
        try {
            OrderPlacedOutbox outbox = new OrderPlacedOutbox();
            outbox.setId(UUID.randomUUID());
            outbox.setOrderId(order.getId());
            outbox.setEventId(event.eventId());
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outboxMapper.insert(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order-placed event for order {}", order.getId(), e);
            throw new BusinessException("Failed to publish order event");
        }
    }

    private OrderResponse toResponse(OrderRow o) {
        return new OrderResponse(o.getId(), o.getInstrumentId(), o.getSymbol(), o.getSide(),
                o.getOrderType(), o.getQuantity(), o.getRequestedPrice(), o.getExecutedPrice(),
                o.getStatus(), o.getRejectReason(), o.getCreatedAt());
    }
}