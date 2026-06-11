package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.dto.RenewalReminderEmailRequest;
import com.josephyusuf.auth.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints service-to-service protégés par {@code X-Internal-Token}.
 * Pas de JWT — utilisés par les crons (subscription-service) qui n'ont
 * pas de contexte HTTP utilisateur.
 */
@RestController
@RequestMapping("/api/auth/users/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final UserManagementService userManagementService;

    @Value("${app.internal.token:}")
    private String expectedToken;

    @PostMapping("/renewal-reminder")
    public ResponseEntity<Void> sendRenewalReminder(@Valid @RequestBody RenewalReminderEmailRequest request,
                                                    HttpServletRequest httpRequest) {
        if (expectedToken == null || expectedToken.isBlank()
                || !expectedToken.equals(httpRequest.getHeader(HEADER_INTERNAL_TOKEN))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userManagementService.sendRenewalReminderEmail(request);
        return ResponseEntity.noContent().build();
    }
}
