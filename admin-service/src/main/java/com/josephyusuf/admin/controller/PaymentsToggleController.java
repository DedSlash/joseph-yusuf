package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.PaymentsToggleActivateResponse;
import com.josephyusuf.admin.dto.PaymentsToggleDeactivateResponse;
import com.josephyusuf.admin.dto.PaymentsToggleStatusDto;
import com.josephyusuf.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
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
                    "Notifié " + response.getUsersNotified() + " utilisateur(s) ("
                            + response.getUsersInOriginalTrial() + " en trial initial, "
                            + response.getUsersInGrace24h() + " en grace 24h)",
                    ip);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<PaymentsToggleDeactivateResponse> deactivate(Authentication auth,
                                                                       HttpServletRequest request) {
        PaymentsToggleDeactivateResponse response = authClient.paymentsToggleDeactivate();
        UUID adminId = UUID.fromString((String) auth.getPrincipal());
        String ip = getIp(request);
        if (!response.isAlreadyInactive()) {
            auditLogService.log(adminId, "PAYMENTS_DEACTIVATED", "SYSTEM_SETTING",
                    "payments.active",
                    "Restauré " + response.getUsersRestored() + " trial(s) ("
                            + response.getUsersExtended() + " étendus, "
                            + response.getUsersInOriginalTrial() + " conservés)",
                    ip);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview-email/{template}")
    public ResponseEntity<Map<String, Object>> previewEmail(@PathVariable String template,
                                                             @RequestParam String to,
                                                             @RequestParam(required = false) String firstName,
                                                             Authentication auth,
                                                             HttpServletRequest request) {
        Map<String, Object> response = authClient.paymentsTogglePreviewEmail(template, to, firstName);
        UUID adminId = UUID.fromString((String) auth.getPrincipal());
        auditLogService.log(adminId, "PAYMENTS_EMAIL_PREVIEW", "SYSTEM_SETTING",
                "payments." + template,
                "Preview '" + template + "' envoyé à " + to,
                getIp(request));
        return ResponseEntity.ok(response);
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
