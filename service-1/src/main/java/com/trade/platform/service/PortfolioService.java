package com.trade.platform.service;

import com.trade.platform.dto.DashboardResponse;
import com.trade.platform.dto.MarketDataDto;
import com.trade.platform.dto.PositionDto;
import com.trade.platform.entity.Account;
import com.trade.platform.entity.Instrument;
import com.trade.platform.entity.Position;
import com.trade.platform.mapper.InstrumentMapper;
import com.trade.platform.mapper.PositionMapper;
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

    private final PositionMapper positionMapper;
    private final InstrumentMapper instrumentMapper;
    private final AccountService accountService;
    private final MarketDataService marketDataService;

    public List<PositionDto> positions(UUID userId) {
        Map<String, MarketDataDto> prices = marketDataService.liveMarketData().stream()
                .collect(toMap(MarketDataDto::symbol, Function.identity(), (a, b) -> a));
        return positionMapper.findByUser(userId).stream().map(p -> toDto(p, prices)).toList();
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
        BigDecimal availableCash = account.getCash();
        BigDecimal total = totalValue.add(availableCash);
        if (total.signum() > 0) {
            dayChangePercent = dayChange.multiply(BigDecimal.valueOf(100))
                    .divide(total, 4, RoundingMode.HALF_UP);
        }
        BigDecimal totalPnl = totalValue.subtract(invested);
        return new DashboardResponse.PortfolioSummary(availableCash, invested, total, dayChange, dayChangePercent, totalPnl);
    }

    private PositionDto toDto(Position p, Map<String, MarketDataDto> prices) {
        Instrument instrument = instrumentMapper.findById(p.getInstrumentId());
        MarketDataDto md = prices.get(p.getSymbol());
        BigDecimal price = md != null ? md.price() : (instrument != null ? instrument.getLastPrice() : BigDecimal.ZERO);
        BigDecimal currentValue = price.multiply(BigDecimal.valueOf(p.getQuantity()));
        BigDecimal pnl = p.getQuantity() > 0
                ? price.subtract(p.getAveragePrice()).multiply(BigDecimal.valueOf(p.getQuantity()))
                : BigDecimal.ZERO;
        BigDecimal pnlPercent = p.getAveragePrice().signum() == 0 ? BigDecimal.ZERO
                : pnl.multiply(BigDecimal.valueOf(100)).divide(p.getAveragePrice().multiply(BigDecimal.valueOf(p.getQuantity())), 2, RoundingMode.HALF_UP);
        return new PositionDto(p.getId(), p.getInstrumentId(), p.getSymbol(), p.getQuantity(),
                p.getAveragePrice(), p.getRealizedPnl(), price, currentValue, pnl, pnlPercent);
    }
}