package com.trade.platform.s2.controller;

import com.trade.platform.s2.dto.InternalDtos;
import com.trade.platform.s2.entity.Account;
import com.trade.platform.s2.mapper.AccountMapper;
import com.trade.platform.s2.mapper.PositionMapper;
import com.trade.platform.s2.mapper.TradeMapper;
import com.trade.platform.s2.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Internal read API consumed only by Service 1 (docker network) for the
 * dashboard/portfolio/account screens. Service 2 is the source of truth for
 * execution state. No external exposure - only the BFF proxies Service 1.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final AccountMapper accountMapper;
    private final PositionMapper positionMapper;
    private final TradeMapper tradeMapper;
    private final TransactionMapper transactionMapper;

    @GetMapping("/accounts/{userId}")
    public InternalDtos.AccountDto account(@PathVariable UUID userId) {
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            // No execution activity yet: report the onboarding default so the
            // dashboard renders the same $1,000,000 the account row seeded.
            return new InternalDtos.AccountDto("ACTIVE", "Trading Account",
                    new BigDecimal("1000000.0000"), BigDecimal.ZERO);
        }
        return new InternalDtos.AccountDto(account.getStatus(), "Trading Account",
                account.getCash(), account.getMargin());
    }

    @GetMapping("/accounts/{userId}/positions")
    public List<InternalDtos.PositionDto> positions(@PathVariable UUID userId) {
        return positionMapper.findByUser(userId).stream()
                .map(p -> new InternalDtos.PositionDto(p.getId(), p.getInstrumentId(), p.getSymbol(),
                        p.getQuantity(), p.getAveragePrice(), p.getRealizedPnl()))
                .toList();
    }

    @GetMapping("/accounts/{userId}/trades")
    public List<InternalDtos.TradeDto> trades(@PathVariable UUID userId) {
        return tradeMapper.findByUser(userId).stream()
                .map(t -> new InternalDtos.TradeDto(t.getId(), t.getOrderId(), t.getSymbol(),
                        t.getSide(), t.getQuantity(), t.getPrice(), t.getExecutedAt()))
                .toList();
    }

    @GetMapping("/accounts/{userId}/transactions")
    public List<InternalDtos.TransactionDto> transactions(@PathVariable UUID userId) {
        return transactionMapper.findByUser(userId).stream()
                .map(t -> new InternalDtos.TransactionDto(t.getId(), t.getOrderId(), t.getType(),
                        t.getAmount(), t.getBalanceAfter(), t.getDescription(), t.getCreatedAt()))
                .toList();
    }
}