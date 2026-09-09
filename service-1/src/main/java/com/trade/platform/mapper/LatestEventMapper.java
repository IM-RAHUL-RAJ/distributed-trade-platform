package com.trade.platform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface LatestEventMapper {

    int insert(@Param("id") UUID id, @Param("eventType") String eventType,
               @Param("orderId") UUID orderId, @Param("payload") String payload);
}