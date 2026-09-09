package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Instrument;
import com.trade.platform.s2.entity.MarketData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MarketDataMapper {

    MarketData findLatest(@Param("instrumentId") UUID instrumentId);

    List<MarketData> findAll();

    Instrument findInstrument(@Param("id") UUID id);

    java.util.List<Instrument> findAllActiveInstruments();

    int upsert(MarketData marketData);
}