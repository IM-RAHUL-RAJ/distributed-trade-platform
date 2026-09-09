package com.trade.platform.s2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.s2.config.TopicProperties;
import com.trade.platform.s2.event.MarketDataEvent;
import com.trade.platform.s2.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TopicProperties topics;
    private final ObjectMapper objectMapper;

    public void publishTradeEvent(TradeEvent event) {
        try {
            kafkaTemplate.send(topics.tradeEvents(), event.orderId().toString(),
                    objectMapper.writeValueAsString(event));
            log.info("Published trade-event order={} status={}", event.orderId(), event.status());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trade-event for order {}", event.orderId(), e);
        }
    }

    public void publishMarketData(MarketDataEvent event) {
        try {
            kafkaTemplate.send(topics.marketData(), event.instrumentId().toString(),
                    objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize market-data for {}", event.symbol(), e);
        }
    }
}