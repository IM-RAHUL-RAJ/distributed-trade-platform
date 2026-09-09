package com.trade.platform.controller;

import com.trade.platform.dto.AccountDto;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountDto get(@CurrentUser UUID userId) {
        return accountService.toDto(userId);
    }
}