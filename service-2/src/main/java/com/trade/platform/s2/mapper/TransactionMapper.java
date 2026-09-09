package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper {

    int insert(Transaction transaction);
}