package com.trade.platform.service;

import com.trade.platform.config.TopicProperties;
import com.trade.platform.entity.OrderPlacedOutbox;
import com.trade.platform.mapper.OutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Polls the transactional outbox and forwards pending order-placed events to
 * Kafka. At-least-once by design; Service 2 deduplicates via processed_events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxMapper outboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TopicProperties topicProperties;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void publishPending() {
        List<OrderPlacedOutbox> pending = outboxMapper.findPending(100);
        if (pending.isEmpty()) {
            return;
        }
        AtomicInteger sent = new AtomicInteger();
        pending.forEach(row -> {
            try {
                kafkaTemplate.send(topicProperties.orderPlaced(), row.getOrderId().toString(), row.getPayload());
                outboxMapper.markPublished(row.getId());
                sent.incrementAndGet();
            } catch (Exception e) {
                // Leave the row PENDING so the next poll retries it.
                log.error("Failed to publish outbox row {} for order {}", row.getId(), row.getOrderId(), e);
            }
        });
        if (sent.get() > 0) {
            log.info("Outbox: published {} order-placed event(s) to Kafka", sent.get());
        }
    }
}