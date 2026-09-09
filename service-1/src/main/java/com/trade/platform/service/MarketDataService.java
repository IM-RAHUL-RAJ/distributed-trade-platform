package com.trade.platform.service;

import com.trade.platform.dto.MarketDataDto;
import com.trade.platform.dto.TranslationDtos;
import com.trade.platform.entity.Instrument;
import com.trade.platform.entity.MarketData;
import com.trade.platform.mapper.InstrumentMapper;
import com.trade.platform.mapper.MarketDataMapper;
import com.trade.platform.mapper.WatchlistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketDataMapper marketDataMapper;
    private final InstrumentMapper instrumentMapper;
    private final WatchlistMapper watchlistMapper;

    public List<MarketDataDto> liveMarketData() {
        Map<String, Instrument> instruments = instrumentMapper.findAllActive().stream()
                .collect(toMap(Instrument::getSymbol, Function.identity(), (a, b) -> a));
        Map<String, MarketData> live = marketDataMapper.findAll().stream()
                .collect(toMap(MarketData::getSymbol, Function.identity(), (a, b) -> a));
        return instruments.values().stream()
                .map(i -> {
                    MarketData md = live.get(i.getSymbol());
                    BigDecimal price = md != null ? md.getPrice() : i.getLastPrice();
                    BigDecimal change = md != null ? md.getChange() : (i.getChange() != null ? i.getChange() : BigDecimal.ZERO);
                    BigDecimal changePercent = md != null ? md.getChangePercent()
                            : (i.getChangePercent() != null ? i.getChangePercent() : BigDecimal.ZERO);
                    return new MarketDataDto(i.getSymbol(), i.getName(), price, change, changePercent);
                })
                .sorted(Comparator.comparing(MarketDataDto::symbol))
                .toList();
    }

    public List<MarketDataDto> watchlist(UUID userId) {
        Map<String, MarketDataDto> live = liveMarketData().stream()
                .collect(toMap(MarketDataDto::symbol, Function.identity(), (a, b) -> a));
        return watchlistMapper.findByUser(userId).stream()
                .map(i -> live.getOrDefault(i.getSymbol(), TranslationDtos.fromInstrument(i)))
                .toList();
    }
}