package com.trade.platform.service;

import com.trade.platform.dto.AccountDto;
import com.trade.platform.dto.S2PositionDto;
import com.trade.platform.dto.TradeDto;
import com.trade.platform.dto.TransactionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * HTTP client for Service 2's internal read API (/api/internal). Service 1 and
 * Service 2 own separate H2 databases; all execution state (cash, positions,
 * trades, transactions) is read from Service 2 over this client.
 */
@Slf4j
@Component
public class Service2Client {

    private final RestClient restClient;

    public Service2Client(@Value("${app.service2.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** @return null if unreachable/unexpected, so callers can fall back. */
    public AccountDto account(UUID userId) {
        try {
            return restClient.get()
                    .uri("/api/internal/accounts/{userId}", userId)
                    .retrieve()
                    .body(AccountDto.class);
        } catch (Exception e) {
            log.warn("Service2 account call failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<S2PositionDto> positions(UUID userId) {
        try {
            return restClient.get()
                    .uri("/api/internal/accounts/{userId}/positions", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<S2PositionDto>>() {
                    });
        } catch (Exception e) {
            log.warn("Service2 positions call failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public List<TradeDto> trades(UUID userId) {
        try {
            return restClient.get()
                    .uri("/api/internal/accounts/{userId}/trades", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TradeDto>>() {
                    });
        } catch (Exception e) {
            log.warn("Service2 trades call failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public List<TransactionDto> transactions(UUID userId) {
        try {
            return restClient.get()
                    .uri("/api/internal/accounts/{userId}/transactions", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TransactionDto>>() {
                    });
        } catch (Exception e) {
            log.warn("Service2 transactions call failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}