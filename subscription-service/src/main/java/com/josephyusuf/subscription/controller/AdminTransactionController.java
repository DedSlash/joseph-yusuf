package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.AdminPageResponse;
import com.josephyusuf.subscription.dto.AdminTransactionDto;
import com.josephyusuf.subscription.service.AdminTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final AdminTransactionService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminPageResponse<AdminTransactionDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(service.list(page, size, status, userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTransactionDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTransactionDto> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(service.refund(id));
    }
}
