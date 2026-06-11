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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth/admin/payments-toggle")
@RequiredArgsConstructor
public class PaymentsToggleController {

    /** Durée de la fenêtre d'essai initiale offerte à la création du compte. */
    private static final int INITIAL_TRIAL_DAYS = 7;

    /** Délai de grâce accordé aux utilisateurs ayant dépassé leur trial initial. */
    private static final int GRACE_PERIOD_HOURS = 24;

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
    @Transactional
    public ResponseEntity<PaymentsToggleActivateResponse> activate() {
        if (systemSettingsService.isPaymentsActive()) {
            return ResponseEntity.ok(PaymentsToggleActivateResponse.builder()
                    .paymentsActive(true)
                    .usersNotified(0)
                    .usersInOriginalTrial(0)
                    .usersInGrace24h(0)
                    .alreadyActive(true)
                    .build());
        }

        List<User> trialUsers = userRepository.findByInTrialTrue();
        systemSettingsService.setPaymentsActive(true);

        LocalDateTime now = LocalDateTime.now();
        int inOriginalTrial = 0;
        int inGrace24h = 0;

        for (User user : trialUsers) {
            LocalDateTime originalEnd = resolveOriginalTrialEnd(user, now);

            if (originalEnd.isAfter(now)) {
                user.setTrialEndsAt(originalEnd);
                userRepository.save(user);
                emailService.sendPaymentsActivatedTrialActive(user);
                trialService.pushInAppAlert(user.getId(),
                        "PAYMENTS_ACTIVATED",
                        "INFO",
                        "Les paiements sont ouverts",
                        "Tu peux souscrire dès maintenant. Ton coupon EARLY50 (-50% à vie) est valable.");
                inOriginalTrial++;
            } else {
                user.setTrialEndsAt(now.plusHours(GRACE_PERIOD_HOURS));
                userRepository.save(user);
                emailService.sendPaymentsActivatedGrace24h(user);
                trialService.pushInAppAlert(user.getId(),
                        "PAYMENTS_GRACE_24H",
                        "WARNING",
                        "24h pour activer ton abonnement",
                        "Tu as dépassé ta fenêtre d'essai initiale de 7 jours. Souscris dans les 24h sinon ton compte passera en FREE.");
                inGrace24h++;
            }
        }

        log.info("Paiements activés par admin — total {} utilisateurs notifiés ({} encore dans trial initial, {} en grace 24h)",
                trialUsers.size(), inOriginalTrial, inGrace24h);

        return ResponseEntity.ok(PaymentsToggleActivateResponse.builder()
                .paymentsActive(true)
                .usersNotified(trialUsers.size())
                .usersInOriginalTrial(inOriginalTrial)
                .usersInGrace24h(inGrace24h)
                .alreadyActive(false)
                .build());
    }

    /**
     * Fin de la fenêtre d'essai initiale (trialStartedAt + 7 jours). Fallback
     * sur now si trialStartedAt est absent — l'utilisateur sera traité comme
     * trial initial dépassé.
     */
    private LocalDateTime resolveOriginalTrialEnd(User user, LocalDateTime now) {
        LocalDateTime startedAt = user.getTrialStartedAt();
        return startedAt != null ? startedAt.plusDays(INITIAL_TRIAL_DAYS) : now;
    }
}
