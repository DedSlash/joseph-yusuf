package com.josephyusuf.income.tips.controller;

import com.josephyusuf.income.tips.dto.MoneyTipsDto;
import com.josephyusuf.income.tips.service.MoneyTipsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes/tips")
@RequiredArgsConstructor
public class MoneyTipsController {

    private final MoneyTipsService moneyTipsService;

    @GetMapping("/{month}/{year}")
    public ResponseEntity<MoneyTipsDto> getTips(Authentication auth,
                                                 HttpServletRequest request,
                                                 @PathVariable int month,
                                                 @PathVariable int year,
                                                 @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        String country = (String) request.getAttribute("country");
        String currency = (String) request.getAttribute("currency");

        Locale locale = parseLocale(acceptLanguage);
        return ResponseEntity.ok(moneyTipsService.getTips(userId, month, year, plan, country, currency, locale));
    }

    private Locale parseLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.FRENCH;
        }
        String primary = acceptLanguage.split(",")[0].trim().split(";")[0].trim();
        if (primary.isEmpty()) return Locale.FRENCH;
        return Locale.forLanguageTag(primary);
    }
}
