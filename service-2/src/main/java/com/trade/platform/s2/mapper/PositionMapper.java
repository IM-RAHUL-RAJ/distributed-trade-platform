package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface PositionMapper {

    Position lockByUserAndInstrument(@Param("userId") UUID userId, @Param("instrumentId") UUID instrumentId);

    int insert(Position position);

    int update(Position position);
}