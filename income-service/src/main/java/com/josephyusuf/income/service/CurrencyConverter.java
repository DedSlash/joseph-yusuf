package com.josephyusuf.income.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyConverter {

    // Taux indicatifs : 1 unité = X XOF. À remplacer par un flux de taux en temps réel.
    private static final Map<String, BigDecimal> RATES_TO_XOF = Map.ofEntries(
        Map.entry("XOF", BigDecimal.ONE),
        Map.entry("XAF", BigDecimal.ONE),
        Map.entry("EUR", new BigDecimal("655.96")),
        Map.entry("USD", new BigDecimal("600")),
        Map.entry("GBP", new BigDecimal("760")),
        Map.entry("CAD", new BigDecimal("445")),
        Map.entry("CHF", new BigDecimal("670")),
        Map.entry("MAD", new BigDecimal("60")),
        Map.entry("DZD", new BigDecimal("4.5")),
        Map.entry("TND", new BigDecimal("190")),
        Map.entry("NGN", new BigDecimal("0.40")),
        Map.entry("GHS", new BigDecimal("42")),
        Map.entry("MRU", new BigDecimal("16")),
        Map.entry("GMD", new BigDecimal("9")),
        Map.entry("SLL", new BigDecimal("0.03")),
        Map.entry("LRD", new BigDecimal("3"))
    );

    public BigDecimal toXOF(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = RATES_TO_XOF.getOrDefault(
            currencyCode != null ? currencyCode.toUpperCase() : "XOF",
            BigDecimal.ONE
        );
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
