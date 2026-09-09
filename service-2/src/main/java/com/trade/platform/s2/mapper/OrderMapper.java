package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.OrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    OrderRow findById(@Param("id") UUID id);

    int insert(OrderRow order);

    int updateStatus(@Param("id") UUID id, @Param("status") String status,
                     @Param("executedPrice") BigDecimal executedPrice,
                     @Param("rejectReason") String rejectReason);
}