package com.trade.platform.mapper;

import com.trade.platform.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface PositionMapper {

    List<Position> findByUser(@Param("userId") UUID userId);

    Position findById(@Param("id") UUID id);
}