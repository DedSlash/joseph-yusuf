package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService service;

    @PostMapping
    public ResponseEntity<PromoCodeResponse> create(@Valid @RequestBody PromoCodeRequest request,
                                                    Authentication auth) {
        UUID adminId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, adminId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PromoCodeResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(page, size, active));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<PromoCodeResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toggle(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<PromoCodeStatsResponse> stats(@PathVariable UUID id) {
        return ResponseEntity.ok(service.stats(id));
    }

    @GetMapping("/validate")
    public ResponseEntity<PromoCodeValidation> validate(@RequestParam String code,
                                                        @RequestParam UUID userId) {
        return ResponseEntity.ok(service.validate(code, userId));
    }

    @PostMapping("/apply")
    public ResponseEntity<PromoCodeValidation> apply(@Valid @RequestBody PromoCodeApplyRequest request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
