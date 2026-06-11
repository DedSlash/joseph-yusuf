package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AlertClient;
import com.josephyusuf.subscription.client.InternalAuthClient;
import com.josephyusuf.subscription.dto.InternalAlertRequest;
import com.josephyusuf.subscription.dto.RenewalReminderEmailRequest;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.RenewalReminderType;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.repository.RenewalReminderRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Envoie les rappels J-3 et J-1 avant expiration d'abonnement, puis un email
 * d'expiration à J+0. PayTech n'offre pas de débit silencieux, donc le
 * renouvellement est forcément manuel — ces rappels sont la seule mécanique
 * pour limiter les abandons.
 *
 * Cron quotidien 9h UTC (≈ 9h Dakar, UTC+0). Dédup portée par la table
 * {@code renewal_reminders} avec contrainte unique
 * (subscription_id, reminder_type, period_end_at).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenewalReminderService {

    private static final long J3_DAYS = 3L;
    private static final long J1_DAYS = 1L;

    private final SubscriptionRepository subscriptionRepository;
    private final RenewalReminderRepository renewalReminderRepository;
    private final RenewalReminderPersister persister;
    private final InternalAuthClient internalAuthClient;
    private final AlertClient alertClient;

    @Scheduled(cron = "${app.renewal-reminders.cron:0 0 9 * * *}")
    public void sendDailyReminders() {
        log.info("Renewal reminders cron start");
        Instant now = Instant.now();

        sendUpcomingReminders(now, J3_DAYS, RenewalReminderType.J_MINUS_3);
        sendUpcomingReminders(now, J1_DAYS, RenewalReminderType.J_MINUS_1);
        sendExpirationEmails(now);

        log.info("Renewal reminders cron end");
    }

    private void sendUpcomingReminders(Instant now, long daysAhead, RenewalReminderType type) {
        Instant windowStart = now.plus(daysAhead - 1, ChronoUnit.DAYS);
        Instant windowEnd = now.plus(daysAhead, ChronoUnit.DAYS);

        List<Subscription> subs = subscriptionRepository.findActiveExpiringBetween(
                SubscriptionStatus.ACTIVE, windowStart, windowEnd);

        for (Subscription sub : subs) {
            attemptSend(sub, type, sub.getExpiresAt(), false);
        }
    }

    private void sendExpirationEmails(Instant now) {
        List<Subscription> expired = subscriptionRepository.findActiveExpiredBefore(
                SubscriptionStatus.ACTIVE, now);
        for (Subscription sub : expired) {
            attemptSend(sub, RenewalReminderType.EXPIRED, sub.getExpiresAt(), true);
        }
    }

    private void attemptSend(Subscription sub, RenewalReminderType type, Instant periodEnd,
                             boolean alsoMarkExpired) {
        if (renewalReminderRepository.existsBySubscriptionIdAndReminderTypeAndPeriodEndAt(
                sub.getId(), type, periodEnd)) {
            return;
        }
        try {
            sendEmail(sub, type);
            sendAlert(sub, type);
            persister.recordSent(sub, type, periodEnd);
            if (alsoMarkExpired) {
                persister.markExpired(sub.getId());
            }
            log.info("Renewal reminder envoyé type={} userId={} subId={}",
                    type, sub.getUserId(), sub.getId());
        } catch (Exception e) {
            // On ne persiste pas la trace si l'envoi a échoué — le cron du
            // lendemain réessaiera. Logguer fort pour visibilité.
            log.error("Échec envoi renewal reminder type={} userId={} subId={} : {}",
                    type, sub.getUserId(), sub.getId(), e.getMessage());
        }
    }

    private void sendEmail(Subscription sub, RenewalReminderType type) {
        internalAuthClient.sendRenewalReminderEmail(RenewalReminderEmailRequest.builder()
                .userId(sub.getUserId())
                .plan(sub.getPlan())
                .type(type)
                .expiresAt(sub.getExpiresAt())
                .couponApplied(sub.getCouponApplied())
                .couponLifetime(sub.isCouponLifetime())
                .build());
    }

    private void sendAlert(Subscription sub, RenewalReminderType type) {
        String title = switch (type) {
            case J_MINUS_3 -> "Abonnement à renouveler dans 3 jours";
            case J_MINUS_1 -> "Plus que 24h pour renouveler";
            case EXPIRED -> "Abonnement expiré";
        };
        String message = switch (type) {
            case J_MINUS_3 -> "Ton abonnement " + sub.getPlan() + " expire le "
                    + sub.getExpiresAt().atZone(ZoneOffset.UTC).toLocalDate()
                    + ". Renouvelle pour conserver ton accès.";
            case J_MINUS_1 -> "Dernier rappel — ton abonnement expire demain.";
            case EXPIRED -> "Ton abonnement a expiré. Tes données sont conservées, "
                    + "réactive quand tu veux.";
        };
        String severity = type == RenewalReminderType.J_MINUS_3 ? "INFO" : "WARNING";

        alertClient.createInternalAlert(InternalAlertRequest.builder()
                .userId(sub.getUserId())
                .type("RENEWAL_REMINDER")
                .severity(severity)
                .title(title)
                .message(message)
                .build());
    }
}
