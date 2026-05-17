package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.client.SubscriptionClient;
import com.josephyusuf.admin.dto.PaymentMethodConfigDto;
import com.josephyusuf.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final SubscriptionClient subscriptionClient;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodConfigDto>> getAll() {
        return ResponseEntity.ok(subscriptionClient.getPaymentMethods());
    }

    @PutMapping("/{provider}/toggle")
    public ResponseEntity<PaymentMethodConfigDto> toggle(@PathVariable String provider,
                                                         Authentication auth,
                                                         HttpServletRequest request) {
        PaymentMethodConfigDto result = subscriptionClient.togglePaymentMethod(provider);
        UUID adminId = UUID.fromString((String) auth.getPrincipal());
        String ip = getIp(request);
        String action = result.isEnabled() ? "PAYMENT_METHOD_ENABLED" : "PAYMENT_METHOD_DISABLED";
        auditLogService.log(adminId, action, "PAYMENT_METHOD", provider, null, ip);
        return ResponseEntity.ok(result);
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
