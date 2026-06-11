package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.entity.RenewalReminder;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.RenewalReminderType;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.repository.RenewalReminderRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistance des effets de bord du cron renewal-reminder, extraite dans un
 * bean dédié pour que les annotations {@code @Transactional} soient
 * effectivement proxifiées par Spring (les self-invocations bypassent l'AOP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenewalReminderPersister {

    private final SubscriptionRepository subscriptionRepository;
    private final RenewalReminderRepository renewalReminderRepository;

    @Transactional
    public void recordSent(Subscription sub, RenewalReminderType type, Instant periodEnd) {
        renewalReminderRepository.save(RenewalReminder.builder()
                .id(UUID.randomUUID())
                .subscriptionId(sub.getId())
                .userId(sub.getUserId())
                .reminderType(type)
                .periodEndAt(periodEnd)
                .sentAt(Instant.now())
                .build());
    }

    @Transactional
    public void markExpired(UUID subscriptionId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription {} marquée EXPIRED (userId={})", sub.getId(), sub.getUserId());
        });
    }
}
