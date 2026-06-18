package com.josephyusuf.income.controller;

import com.josephyusuf.income.service.CurrencyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/incomes/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyConverter currencyConverter;

    @GetMapping("/rates")
    public ResponseEntity<Map<String, BigDecimal>> getRates() {
        return ResponseEntity.ok(currencyConverter.getRatesToXof());
    }
}
