package com.trade.platform.mapper;

import com.trade.platform.entity.MarketData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MarketDataMapper {

    List<MarketData> findAll();

    MarketData findByInstrument(@Param("instrumentId") UUID instrumentId);

    int upsert(@Param("instrumentId") UUID instrumentId, @Param("symbol") String symbol,
               @Param("price") BigDecimal price, @Param("change") BigDecimal change,
               @Param("changePercent") BigDecimal changePercent, @Param("timestamp") Instant timestamp);
}