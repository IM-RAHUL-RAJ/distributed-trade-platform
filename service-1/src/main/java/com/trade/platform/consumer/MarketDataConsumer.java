package com.trade.platform.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.platform.event.MarketDataEvent;
import com.trade.platform.mapper.InstrumentMapper;
import com.trade.platform.mapper.MarketDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataConsumer {

    private final MarketDataMapper marketDataMapper;
    private final InstrumentMapper instrumentMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topics.market-data}", groupId = "service-1-group")
    public void onMarketData(String payload) {
        try {
            MarketDataEvent event = objectMapper.readValue(payload, MarketDataEvent.class);
            if (event.instrumentId() == null) {
                log.warn("MarketData event without instrumentId, skipping");
                return;
            }
            marketDataMapper.upsert(event.instrumentId(), event.symbol(), event.price(),
                    event.change(), event.changePercent(), event.timestamp());
            instrumentMapper.updatePrice(event.instrumentId(), event.instrumentId(), event.symbol(),
                    event.price(), event.change(), event.changePercent());
            log.debug("Applied market-data for {} @ {}", event.symbol(), event.price());
        } catch (Exception e) {
            log.error("Failed to process market-data event", e);
        }
    }
}