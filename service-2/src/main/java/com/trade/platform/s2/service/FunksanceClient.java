package com.trade.platform.s2.service;

import com.trade.platform.s2.config.FunksanceProperties;
import com.trade.platform.s2.entity.MarketData;
import com.trade.platform.s2.entity.Instrument;
import com.trade.platform.s2.mapper.MarketDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Client for the external Funksance market-data API. Only Service 2 talks to
 * Funksance; it is never exposed to the UI.
 */
@Slf4j
@Service
public class FunksanceClient {

    private final RestClient restClient;
    private final MarketDataMapper marketDataMapper;

    public FunksanceClient(FunksanceProperties properties, MarketDataMapper marketDataMapper) {
        this.restClient = RestClient.builder().baseUrl(properties.apiUrl()).build();
        this.marketDataMapper = marketDataMapper;
    }

    public List<MarketData> pull() {
        try {
            FunksanceResponse[] quotes = restClient.get()
                    .uri("/prices")
                    .retrieve()
                    .body(FunksanceResponse[].class);
            if (quotes == null) {
                return List.of();
            }
            Map<String, Instrument> instruments = marketDataMapper.findAllActiveInstruments().stream()
                    .collect(toMap(Instrument::getSymbol, Function.identity(), (a, b) -> a));
            List<MarketData> rows = new java.util.ArrayList<>();
            for (FunksanceResponse q : quotes) {
                Instrument instrument = instruments.get(q.symbol());
                if (instrument == null) {
                    continue;
                }
                MarketData md = new MarketData();
                md.setInstrumentId(instrument.getId());
                md.setSymbol(q.symbol());
                md.setPrice(q.price());
                md.setChange(q.change() != null ? q.change() : BigDecimal.ZERO);
                md.setChangePercent(q.changePercent() != null ? q.changePercent() : BigDecimal.ZERO);
                md.setTimestamp(java.time.Instant.now());
                rows.add(md);
            }
            return rows;
        } catch (Exception e) {
            log.warn("Funksance pull failed: {}", e.getMessage());
            return List.of();
        }
    }

    public record FunksanceResponse(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent) {
    }
}