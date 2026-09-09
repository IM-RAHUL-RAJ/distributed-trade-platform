package com.trade.platform.mapper;

import com.trade.platform.entity.Instrument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface InstrumentMapper {

    List<Instrument> findAllActive();

    Instrument findById(@Param("id") UUID id);

    Instrument findBySymbol(@Param("symbol") String symbol);

    List<Instrument> findBySymbols(@Param("symbols") List<String> symbols);

    int updatePrice(@Param("id") UUID id, @Param("instrumentId") UUID instrumentId,
                    @Param("symbol") String symbol, @Param("price") java.math.BigDecimal price,
                    @Param("change") java.math.BigDecimal change,
                    @Param("changePercent") java.math.BigDecimal changePercent);
}