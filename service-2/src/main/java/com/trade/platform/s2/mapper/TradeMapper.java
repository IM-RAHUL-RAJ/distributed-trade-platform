package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Trade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface TradeMapper {

    Trade findByOrderId(@Param("orderId") UUID orderId);

    int insert(Trade trade);
}