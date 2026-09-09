package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TransactionMapper {

    List<Transaction> findByUser(@Param("userId") UUID userId);

    int insert(Transaction transaction);
}