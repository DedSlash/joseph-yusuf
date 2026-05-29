package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.PaymentsToggleActivateResponse;
import com.josephyusuf.admin.dto.PaymentsToggleStatusDto;
import com.josephyusuf.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments-toggle")
@RequiredArgsConstructor
public class PaymentsToggleController {

    private final AuthClient authClient;
    private final AuditLogService auditLogService;

    @GetMapping("/status")
    public ResponseEntity<PaymentsToggleStatusDto> status() {
        return ResponseEntity.ok(authClient.paymentsToggleStatus());
    }

    @PostMapping("/activate")
    public ResponseEntity<PaymentsToggleActivateResponse> activate(Authentication auth,
                                                                   HttpServletRequest request) {
        PaymentsToggleActivateResponse response = authClient.paymentsToggleActivate();
        UUID adminId = UUID.fromString((String) auth.getPrincipal());
        String ip = getIp(request);
        if (!response.isAlreadyActive()) {
            auditLogService.log(adminId, "PAYMENTS_ACTIVATED", "SYSTEM_SETTING",
                    "payments.active",
                    "Notifié " + response.getUsersNotified() + " utilisateur(s) en prolongation",
                    ip);
        }
        return ResponseEntity.ok(response);
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
