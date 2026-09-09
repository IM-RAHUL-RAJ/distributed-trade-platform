package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PositionMapper {

    Position lockByUserAndInstrument(@Param("userId") UUID userId, @Param("instrumentId") UUID instrumentId);

    List<Position> findByUser(@Param("userId") UUID userId);

    int insert(Position position);

    int update(Position position);
}