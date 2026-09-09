package com.trade.platform.s2.mapper;

import com.trade.platform.s2.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface AccountMapper {

    Account lockById(@Param("id") UUID id);

    Account findByUserId(@Param("userId") UUID userId);

    int insert(Account account);

    int updateCash(@Param("id") UUID id, @Param("cash") BigDecimal cash);
}