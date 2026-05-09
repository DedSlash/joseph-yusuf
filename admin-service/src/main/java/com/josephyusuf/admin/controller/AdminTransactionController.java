package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.dto.TransactionDto;
import com.josephyusuf.admin.service.AdminTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final AdminTransactionService service;

    @GetMapping
    public ResponseEntity<PageResponse<TransactionDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(service.list(page, size, status, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TransactionDto> refund(@PathVariable UUID id,
                                                 Authentication auth,
                                                 HttpServletRequest http) {
        return ResponseEntity.ok(service.refund(id, adminId(auth), clientIp(http)));
    }

    private UUID adminId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
