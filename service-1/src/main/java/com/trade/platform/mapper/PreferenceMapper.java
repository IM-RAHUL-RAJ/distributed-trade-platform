package com.trade.platform.mapper;

import com.trade.platform.entity.CustomerPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface PreferenceMapper {

    CustomerPreference findByUserId(@Param("userId") UUID userId);

    int insert(CustomerPreference preference);

    int update(CustomerPreference preference);

    int insertWithCompletion(CustomerPreference preference);
}