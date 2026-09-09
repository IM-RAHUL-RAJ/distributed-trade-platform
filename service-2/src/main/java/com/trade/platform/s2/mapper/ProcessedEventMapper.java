package com.trade.platform.s2.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface ProcessedEventMapper {

    /**
     * Inserts an event into the idempotency ledger. Returns 1 if inserted,
     * 0 if the eventId was already seen (duplicate).
     */
    int insertIgnore(@Param("eventId") String eventId, @Param("eventType") String eventType,
                     @Param("orderId") UUID orderId, @Param("payload") String payload);
}