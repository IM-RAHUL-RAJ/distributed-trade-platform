package com.trade.platform.service;

import com.trade.platform.dto.InstrumentDto;
import com.trade.platform.entity.Instrument;
import com.trade.platform.mapper.InstrumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentMapper instrumentMapper;

    public List<InstrumentDto> list() {
        return instrumentMapper.findAllActive().stream().map(this::toDto).toList();
    }

    public Optional<Instrument> findBySymbol(String symbol) {
        return Optional.ofNullable(instrumentMapper.findBySymbol(symbol));
    }

    public InstrumentDto toDto(Instrument i) {
        return new InstrumentDto(i.getId(), i.getSymbol(), i.getName(), i.getExchange(),
                i.getCurrency(), i.getType(), i.getLastPrice(), i.getChange(), i.getChangePercent());
    }
}