package com.trade.platform.s2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.funksance")
public record FunksanceProperties(String apiUrl, long pollIntervalMs) {
}