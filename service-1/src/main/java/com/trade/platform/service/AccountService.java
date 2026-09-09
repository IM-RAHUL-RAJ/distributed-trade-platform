package com.trade.platform.service;

import com.trade.platform.common.BusinessException;
import com.trade.platform.dto.AccountDto;
import com.trade.platform.entity.Account;
import com.trade.platform.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final Service2Client service2Client;
    private final BigDecimal defaultCash;
    private final List<String> watchlistSymbols;

    public AccountService(AccountMapper accountMapper,
                          Service2Client service2Client,
                          @Value("${app.onboarding.default-cash:1000000}") BigDecimal defaultCash,
                          @Value("${app.onboarding.watchlist-symbols:NVDA,AAPL,MSFT,TSLA,META,GOOG,AMZN}") String watchlistSymbols) {
        this.accountMapper = accountMapper;
        this.service2Client = service2Client;
        this.defaultCash = defaultCash;
        this.watchlistSymbols = Arrays.stream(watchlistSymbols.split(",")).map(String::trim).toList();
    }

    /**
     * Onboarding: lazily creates the trading account and default watchlist the
     * first time a user touches any business endpoint. Business logic lives here,
     * NOT in the BFF.
     */
    @Transactional
    public Account getOrCreate(UUID userId) {
        Account account = accountMapper.findByUserId(userId);
        if (account != null) {
            return account;
        }
        accountMapper.insertDefaultAccount(userId, defaultCash);
        accountMapper.insertDefaultWatchlist(userId, watchlistSymbols);
        account = accountMapper.findByUserId(userId);
        log.info("Onboarded account {} with {} cash for user {}", account.getId(), defaultCash, userId);
        return account;
    }

    /**
     * Cash/margin always come from Service 2 (the execution ledger). The local
     * accounts table is only kept for FK integrity on orders in the read model.
     */
    public AccountDto toDto(UUID userId) {
        Account account = getOrCreate(userId);
        AccountDto remote = service2Client.account(userId);
        if (remote != null) {
            return remote;
        }
        return new AccountDto(account.getStatus(), "Trading Account", account.getCash(), account.getMargin());
    }
}