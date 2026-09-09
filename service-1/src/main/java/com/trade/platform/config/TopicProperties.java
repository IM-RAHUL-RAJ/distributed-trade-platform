package com.trade.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.topics")
public record TopicProperties(String orderPlaced, String tradeEvents, String marketData) {
}