package com.trade.platform.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.event.TradeEvent;
import com.trade.platform.mapper.LatestEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final LatestEventMapper latestEventMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topics.trade-events}", groupId = "service-1-group")
    public void onTradeEvent(String payload) {
        try {
            TradeEvent event = objectMapper.readValue(payload, TradeEvent.class);
            latestEventMapper.insert(UUID.randomUUID(), "trade-event", event.orderId(), payload);
            log.info("Recorded trade-event order={} status={} price={}",
                    event.orderId(), event.status(), event.executedPrice());
        } catch (Exception e) {
            log.error("Failed to persist trade-event", e);
        }
    }
}