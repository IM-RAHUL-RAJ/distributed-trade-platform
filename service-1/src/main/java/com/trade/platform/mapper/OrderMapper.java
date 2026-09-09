package com.trade.platform.mapper;

import com.trade.platform.entity.OrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    int insert(OrderRow order);

    OrderRow findById(@Param("id") UUID id);

    OrderRow findByIdAndUser(@Param("id") UUID id, @Param("userId") UUID userId);

    List<OrderRow> findByUser(@Param("userId") UUID userId);

    List<OrderRow> findOpenByUser(@Param("userId") UUID userId);

    int updateStatus(@Param("id") UUID id, @Param("status") String status,
                     @Param("executedPrice") java.math.BigDecimal executedPrice,
                     @Param("rejectReason") String rejectReason);
}