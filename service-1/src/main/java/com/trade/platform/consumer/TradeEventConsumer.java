package com.trade.platform.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.event.TradeEvent;
import com.trade.platform.mapper.LatestEventMapper;
import com.trade.platform.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Records trade-events into the latest_events audit table AND applies the
 * terminal order status (EXECUTED / REJECTED) to Service 1's order read model.
 * The shared-database write from Service 2 is gone - this is the only path
 * that updates order status now.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final LatestEventMapper latestEventMapper;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topics.trade-events}", groupId = "service-1-group")
    public void onTradeEvent(String payload) {
        try {
            TradeEvent event = objectMapper.readValue(payload, TradeEvent.class);
            latestEventMapper.insert(UUID.randomUUID(), "trade-event", event.orderId(), payload);
            if ("EXECUTED".equals(event.status())) {
                orderMapper.updateStatus(event.orderId(), "EXECUTED", event.executedPrice(), null);
            } else if ("REJECTED".equals(event.status())) {
                orderMapper.updateStatus(event.orderId(), "REJECTED", null, event.rejectReason());
            }
            log.info("Recorded trade-event order={} status={} price={}",
                    event.orderId(), event.status(), event.executedPrice());
        } catch (Exception e) {
            log.error("Failed to persist trade-event", e);
        }
    }
}