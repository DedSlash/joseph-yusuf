package com.josephyusuf.report.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Conversion XOF → devise utilisateur pour l'affichage dans les PDF.
 * Duplique les taux de {@code income-service/CurrencyConverter} : le report-service
 * stocke et calcule en XOF, ne convertit qu'à l'affichage. Source unique de vérité
 * fonctionnelle : la parité fixe BCEAO 1 EUR = 655.957 XOF.
 */
@Service
public class ReportCurrencyConverter {

    private static final Map<String, BigDecimal> RATES_TO_XOF = Map.ofEntries(
        Map.entry("XOF", BigDecimal.ONE),
        Map.entry("XAF", BigDecimal.ONE),
        Map.entry("EUR", new BigDecimal("655.957")),
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

    public BigDecimal fromXOF(BigDecimal amountXof, String currencyCode) {
        if (amountXof == null || amountXof.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        String code = currencyCode != null ? currencyCode.toUpperCase() : "XOF";
        BigDecimal rate = RATES_TO_XOF.getOrDefault(code, BigDecimal.ONE);
        int scale = isZeroDecimal(code) ? 0 : 2;
        return amountXof.divide(rate, scale, RoundingMode.HALF_UP);
    }

    public String displayCode(String currencyCode) {
        if (currencyCode == null) return "XOF";
        String code = currencyCode.toUpperCase();
        return "XOF".equals(code) ? "FCFA" : code;
    }

    private boolean isZeroDecimal(String code) {
        return "XOF".equals(code) || "XAF".equals(code);
    }
}
