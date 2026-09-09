package com.trade.platform.mapper;

import com.trade.platform.entity.Instrument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface WatchlistMapper {

    List<Instrument> findByUser(@Param("userId") UUID userId);
}