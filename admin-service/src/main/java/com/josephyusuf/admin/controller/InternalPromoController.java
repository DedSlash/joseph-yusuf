package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.dto.PromoCodeApplyRequest;
import com.josephyusuf.admin.dto.PromoCodeValidation;
import com.josephyusuf.admin.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interne service-to-service, non exposé via gateway.
 * Permet à subscription-service d'enregistrer l'usage d'un code promo
 * sans JWT utilisateur (appel déclenché par webhook Stripe).
 */
@RestController
@RequestMapping("/api/internal/promo-codes")
@RequiredArgsConstructor
public class InternalPromoController {

    private final PromoCodeService service;

    @PostMapping("/apply")
    public ResponseEntity<PromoCodeValidation> apply(@Valid @RequestBody PromoCodeApplyRequest request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
