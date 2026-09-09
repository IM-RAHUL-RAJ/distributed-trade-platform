package com.trade.platform.s2.config;

import org.apache.ibatis.type.InstantTypeHandler;
import org.apache.ibatis.type.LocalDateTypeHandler;
import org.apache.ibatis.type.LocalDateTimeTypeHandler;
import org.apache.ibatis.type.OffsetDateTimeTypeHandler;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Configuration
public class MybatisConfig {

    @Bean
    public ConfigurationCustomizer typeHandlerCustomizer() {
        return cfg -> {
            // MyBatis 3.5.9+ dropped auto-registration of UUID + JSR-310 handlers.
            cfg.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
            cfg.getTypeHandlerRegistry().register(Instant.class, new InstantTypeHandler());
            cfg.getTypeHandlerRegistry().register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());
            cfg.getTypeHandlerRegistry().register(LocalDateTime.class, new LocalDateTimeTypeHandler());
            cfg.getTypeHandlerRegistry().register(LocalDate.class, new LocalDateTypeHandler());
        };
    }
}