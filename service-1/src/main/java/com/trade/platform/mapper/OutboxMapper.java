package com.trade.platform.mapper;

import com.trade.platform.entity.OrderPlacedOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OutboxMapper {

    int insert(OrderPlacedOutbox outbox);

    List<OrderPlacedOutbox> findPending(@Param("limit") int limit);

    int markPublished(@Param("id") UUID id);
}