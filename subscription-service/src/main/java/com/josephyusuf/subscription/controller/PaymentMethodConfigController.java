package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.dto.PaymentMethodConfigDto;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.dto.PromoCodePublicValidationResponse;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.service.PaymentMethodConfigService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentMethodConfigController {

    private final PaymentMethodConfigService service;
    private final AdminClient adminClient;

    // Accessible par tous les utilisateurs authentifiés — pour le frontend client
    // Retourne seulement les providers activés et affichables (hors agrégateurs internes).
    @GetMapping("/api/subscriptions/payment-methods")
    public ResponseEntity<List<PaymentMethodConfigDto>> getAvailable() {
        return ResponseEntity.ok(service.getAvailableForClient());
    }

    // Validation code promo accessible à tous les utilisateurs connectés
    @GetMapping("/api/subscriptions/promo-codes/validate")
    public ResponseEntity<PromoCodeValidation> validatePromo(@RequestParam String code,
                                                              Authentication auth) {
        java.util.UUID userId = java.util.UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(adminClient.validate(code, userId));
    }

    @GetMapping("/api/subscriptions/promo-codes/validate-public")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PromoCodePublicValidationResponse> validatePromoPublic(@RequestParam String code) {
        log.info("Validation publique code promo: {}", code);
        try {
            PromoCodeValidation validation = adminClient.validatePublic(code);
            return ResponseEntity.ok(PromoCodePublicValidationResponse.from(validation));
        } catch (FeignException e) {
            log.warn("Erreur validation publique code promo '{}': {}", code, e.getMessage());
            return ResponseEntity.ok(PromoCodePublicValidationResponse.notFound());
        }
    }

    // Accessible admin uniquement — pour le dashboard admin
    @GetMapping("/api/subscriptions/admin/payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentMethodConfigDto>> getAllAdmin() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/api/subscriptions/admin/payment-methods/{provider}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentMethodConfigDto> toggle(@PathVariable PaymentProvider provider) {
        return ResponseEntity.ok(service.toggle(provider));
    }
}
