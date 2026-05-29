package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.dto.PaymentsToggleActivateResponse;
import com.josephyusuf.auth.dto.PaymentsToggleStatusDto;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import com.josephyusuf.auth.service.EmailService;
import com.josephyusuf.auth.service.SystemSettingsService;
import com.josephyusuf.auth.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth/admin/payments-toggle")
@RequiredArgsConstructor
public class PaymentsToggleController {

    private final SystemSettingsService systemSettingsService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TrialService trialService;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentsToggleStatusDto> status() {
        return ResponseEntity.ok(PaymentsToggleStatusDto.builder()
                .paymentsActive(systemSettingsService.isPaymentsActive())
                .usersInTrialExtension(userRepository.countByInTrialTrue())
                .build());
    }

    @PostMapping("/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentsToggleActivateResponse> activate() {
        if (systemSettingsService.isPaymentsActive()) {
            return ResponseEntity.ok(PaymentsToggleActivateResponse.builder()
                    .paymentsActive(true)
                    .usersNotified(0)
                    .alreadyActive(true)
                    .build());
        }

        List<User> trialUsers = userRepository.findByInTrialTrue();
        systemSettingsService.setPaymentsActive(true);

        for (User user : trialUsers) {
            emailService.sendPaymentsActivated(user);
            trialService.pushInAppAlert(user.getId(),
                    "PAYMENTS_ACTIVATED",
                    "INFO",
                    "Les paiements sont ouverts",
                    "Tu peux activer ton abonnement Premium ou Premium+. Ton coupon EARLY50 (-50% à vie) reste valable.");
        }

        log.info("Paiements activés par admin — {} utilisateurs notifiés", trialUsers.size());

        return ResponseEntity.ok(PaymentsToggleActivateResponse.builder()
                .paymentsActive(true)
                .usersNotified(trialUsers.size())
                .alreadyActive(false)
                .build());
    }
}
