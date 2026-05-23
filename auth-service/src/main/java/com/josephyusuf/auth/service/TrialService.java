package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        List<User> expiredTrials = userRepository
                .findByInTrialTrueAndTrialEndsAtBefore(LocalDateTime.now());

        for (User user : expiredTrials) {
            user.setPlan(Plan.FREE);
            user.setInTrial(false);
            userRepository.save(user);
            emailService.sendTrialExpired(user);
            log.info("Trial expiré pour userId={}, downgrade vers FREE", user.getId());
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendTrialReminders() {
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

    public record TrialStatus(
            boolean isInTrial,
            LocalDateTime trialEndsAt,
            int daysRemaining,
            int hoursRemaining,
            boolean trialUsed
    ) {}
}
