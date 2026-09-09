package com.trade.platform.service;

import com.trade.platform.dto.DashboardResponse;
import com.trade.platform.dto.MarketDataDto;
import com.trade.platform.dto.PositionDto;
import com.trade.platform.dto.S2PositionDto;
import com.trade.platform.entity.Account;
import com.trade.platform.entity.Instrument;
import com.trade.platform.mapper.InstrumentMapper;
import com.trade.platform.service.Service2Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final InstrumentMapper instrumentMapper;
    private final AccountService accountService;
    private final MarketDataService marketDataService;
    private final Service2Client service2Client;

    public List<PositionDto> positions(UUID userId) {
        Map<String, MarketDataDto> prices = marketDataService.liveMarketData().stream()
                .collect(toMap(MarketDataDto::symbol, Function.identity(), (a, b) -> a));
        return service2Client.positions(userId).stream().map(p -> toDto(p, prices)).toList();
    }

    /** Holdings = positions with non-zero quantity. */
    public List<PositionDto> holdings(UUID userId) {
        return positions(userId).stream().filter(p -> p.quantity() > 0).toList();
    }

    public DashboardResponse.PortfolioSummary summary(UUID userId) {
        Account account = accountService.getOrCreate(userId);
        List<PositionDto> holdings = holdings(userId);

        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal dayChange = BigDecimal.ZERO;
        BigDecimal dayChangePercent = BigDecimal.ZERO;
        for (PositionDto h : holdings) {
            invested = invested.add(h.averagePrice().multiply(BigDecimal.valueOf(h.quantity())));
            totalValue = totalValue.add(h.currentValue());
            dayChange = dayChange.add(h.pnl());
        }
        BigDecimal availableCash = accountService.toDto(userId).cash();
        BigDecimal total = totalValue.add(availableCash);
        if (total.signum() > 0) {
            dayChangePercent = dayChange.multiply(BigDecimal.valueOf(100))
                    .divide(total, 4, RoundingMode.HALF_UP);
        }
        BigDecimal totalPnl = totalValue.subtract(invested);
        return new DashboardResponse.PortfolioSummary(availableCash, invested, total, dayChange, dayChangePercent, totalPnl);
    }

    private PositionDto toDto(S2PositionDto p, Map<String, MarketDataDto> prices) {
        Instrument instrument = instrumentMapper.findById(p.instrumentId());
        MarketDataDto md = prices.get(p.symbol());
        BigDecimal price = md != null ? md.price() : (instrument != null ? instrument.getLastPrice() : BigDecimal.ZERO);
        BigDecimal currentValue = price.multiply(BigDecimal.valueOf(p.quantity()));
        BigDecimal pnl = p.quantity() > 0
                ? price.subtract(p.averagePrice()).multiply(BigDecimal.valueOf(p.quantity()))
                : BigDecimal.ZERO;
        BigDecimal pnlPercent = p.averagePrice().signum() == 0 ? BigDecimal.ZERO
                : pnl.multiply(BigDecimal.valueOf(100)).divide(p.averagePrice().multiply(BigDecimal.valueOf(p.quantity())), 2, RoundingMode.HALF_UP);
        return new PositionDto(p.id(), p.instrumentId(), p.symbol(), p.quantity(),
                p.averagePrice(), p.realizedPnl(), price, currentValue, pnl, pnlPercent);
    }
}