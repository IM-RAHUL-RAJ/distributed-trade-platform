package com.trade.platform.s2.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TopicProperties.class, FunksanceProperties.class})
public class S2Config {
}