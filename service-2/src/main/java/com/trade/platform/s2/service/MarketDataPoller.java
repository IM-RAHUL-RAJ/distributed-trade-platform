package com.trade.platform.s2.service;

import com.trade.platform.s2.entity.MarketData;
import com.trade.platform.s2.event.MarketDataEvent;
import com.trade.platform.s2.mapper.MarketDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Polls Funksance on a configurable interval, persists the latest quotes into
 * market_data (source of execution prices) and publishes normalized
 * market-data events to Kafka for Service 1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataPoller {

    private final FunksanceClient funksanceClient;
    private final KafkaPublisher kafkaPublisher;
    private final MarketDataMapper marketDataMapper;

    @Scheduled(fixedDelayString = "${app.funksance.poll-interval-ms:5000}")
    public void poll() {
        List<MarketData> quotes = funksanceClient.pull();
        if (quotes.isEmpty()) {
            return;
        }
        int published = 0;
        for (MarketData quote : quotes) {
            marketDataMapper.upsert(quote);
            MarketDataEvent event = new MarketDataEvent(
                    UUID.randomUUID(),
                    quote.getInstrumentId(),
                    quote.getSymbol(),
                    quote.getPrice(),
                    quote.getChange(),
                    quote.getChangePercent(),
                    Instant.now());
            kafkaPublisher.publishMarketData(event);
            published++;
        }
        log.info("Market data poll: {} instrument(s) refreshed", published);
    }
}