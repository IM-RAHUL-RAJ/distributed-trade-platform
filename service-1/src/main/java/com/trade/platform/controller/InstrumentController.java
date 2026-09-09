package com.trade.platform.controller;

import com.trade.platform.dto.InstrumentDto;
import com.trade.platform.service.InstrumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping
    public List<InstrumentDto> list() {
        return instrumentService.list();
    }
}