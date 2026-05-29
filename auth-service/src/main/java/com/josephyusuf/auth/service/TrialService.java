package com.josephyusuf.auth.service;

import com.josephyusuf.auth.client.AlertClient;
import com.josephyusuf.auth.client.dto.InternalAlertRequest;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SystemSettingsService systemSettingsService;
    private final AlertClient alertClient;

    @Value("${app.trial.extension-days:30}")
    private int extensionDays;

    @Transactional
    public void startTrial(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (user.isTrialUsed()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        user.setTrialStartedAt(now);
        user.setTrialEndsAt(now.plusDays(7));
        user.setInTrial(true);
        user.setTrialUsed(true);
        user.setPlan(Plan.PREMIUM_PLUS);
        userRepository.save(user);

        emailService.sendTrialWelcome(user);
        log.info("Trial PREMIUM_PLUS démarré pour userId={}, expire le {}", userId, user.getTrialEndsAt());
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireTrials() {
        boolean paymentsActive = systemSettingsService.isPaymentsActive();
        List<User> expiredTrials = userRepository
                .findByInTrialTrueAndTrialEndsAtBefore(LocalDateTime.now());

        for (User user : expiredTrials) {
            if (paymentsActive) {
                user.setPlan(Plan.FREE);
                user.setInTrial(false);
                userRepository.save(user);
                emailService.sendTrialExpired(user);
                log.info("Trial expiré pour userId={}, downgrade vers FREE", user.getId());
            } else {
                LocalDateTime newEnd = LocalDateTime.now().plusDays(extensionDays);
                user.setTrialEndsAt(newEnd);
                userRepository.save(user);
                emailService.sendTrialExtended(user);
                pushInAppAlert(user.getId(),
                        "TRIAL_EXTENDED",
                        "SUCCESS",
                        "Ton accès Premium+ continue",
                        "Ta période d'essai est prolongée gratuitement jusqu'à l'ouverture officielle des paiements. Ton coupon EARLY50 reste réservé.");
                log.info("Trial prolongé de {} jours pour userId={} (PAYMENTS_ACTIVE=false), nouvelle fin: {}",
                        extensionDays, user.getId(), newEnd);
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendTrialReminders() {
        if (!systemSettingsService.isPaymentsActive()) {
            log.debug("Rappel J-1 court-circuité: PAYMENTS_ACTIVE=false (prolongation auto)");
            return;
        }

        LocalDateTime tomorrow = LocalDateTime.now().plusHours(24);
        LocalDateTime dayAfterTomorrow = tomorrow.plusHours(24);

        List<User> expiringTomorrow = userRepository
                .findByInTrialTrueAndTrialEndsAtBetween(tomorrow, dayAfterTomorrow);

        for (User user : expiringTomorrow) {
            emailService.sendTrialReminder(user);
            log.info("Rappel J-1 envoyé à userId={}", user.getId());
        }
    }

    @Transactional(readOnly = true)
    public TrialStatus getTrialStatus(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!user.isInTrial()) {
            return new TrialStatus(false, null, 0, 0, user.isTrialUsed());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endsAt = user.getTrialEndsAt();
        long hoursRemaining = java.time.Duration.between(now, endsAt).toHours();
        int daysRemaining = (int) Math.max(0, java.time.Duration.between(now, endsAt).toDays());

        return new TrialStatus(true, endsAt, daysRemaining, (int) Math.max(0, hoursRemaining), user.isTrialUsed());
    }

    public void pushInAppAlert(UUID userId, String type, String severity, String title, String message) {
        try {
            alertClient.createInternalAlert(InternalAlertRequest.builder()
                    .userId(userId)
                    .type(type)
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .build());
        } catch (Exception e) {
            log.warn("Échec création alerte in-app userId={} type={}: {}", userId, type, e.getMessage());
        }
    }

    public record TrialStatus(
            boolean isInTrial,
            LocalDateTime trialEndsAt,
            int daysRemaining,
            int hoursRemaining,
            boolean trialUsed
    ) {}
}
