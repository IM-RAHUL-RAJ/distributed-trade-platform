package com.trade.platform.controller;

import com.trade.platform.dto.OrderRequest;
import com.trade.platform.dto.OrderResponse;
import com.trade.platform.security.CurrentUser;
import com.trade.platform.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@CurrentUser UUID userId, @Valid @RequestBody OrderRequest request) {
        return orderService.create(userId, request);
    }

    @GetMapping
    public List<OrderResponse> list(@CurrentUser UUID userId) {
        return orderService.list(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return orderService.get(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@CurrentUser UUID userId, @PathVariable UUID id) {
        return orderService.cancel(userId, id);
    }
}