package com.trade.platform.mapper;

import com.trade.platform.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface AccountMapper {

    Account findById(@Param("id") UUID id);

    Account findByUserId(@Param("userId") UUID userId);

    int insert(Account account);

    int updateCash(@Param("id") UUID id, @Param("cash") BigDecimal cash);

    int insertDefaultAccount(@Param("userId") UUID userId, @Param("cash") BigDecimal cash);

    int insertDefaultWatchlist(@Param("userId") UUID userId, @Param("symbols") java.util.List<String> symbols);
}