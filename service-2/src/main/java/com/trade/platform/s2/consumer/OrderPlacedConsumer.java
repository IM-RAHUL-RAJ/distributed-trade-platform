package com.trade.platform.s2.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.s2.event.OrderPlacedEvent;
import com.trade.platform.s2.event.TradeEvent;
import com.trade.platform.s2.service.KafkaPublisher;
import com.trade.platform.s2.service.OrderExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedConsumer {

    private final OrderExecutionService executionService;
    private final KafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topics.order-placed}", groupId = "service-2-executor")
    public void onOrderPlaced(String payload) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            OrderExecutionService.ExecutionResult result = executionService.execute(event, payload);

            if (result.shouldPublish()) {
                TradeEvent tradeEvent = new TradeEvent(
                        UUID.randomUUID(),
                        result.orderId(),
                        event.userId(),
                        event.instrumentId(),
                        event.symbol(),
                        event.side(),
                        event.quantity(),
                        event.requestedPrice(),
                        result.executedPrice() != null ? result.executedPrice() : event.requestedPrice(),
                        result.outcome(),
                        result.rejectReason(),
                        Instant.now());
                kafkaPublisher.publishTradeEvent(tradeEvent);
            }
        } catch (Exception e) {
            // Lets the consumer retry/redeliver; execution is idempotent in DB.
            log.error("Failed to process order-placed event", e);
            throw new RuntimeException(e);
        }
    }
}