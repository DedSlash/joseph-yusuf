package com.josephyusuf.subscription.repository;

import com.josephyusuf.subscription.entity.RenewalReminder;
import com.josephyusuf.subscription.enums.RenewalReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface RenewalReminderRepository extends JpaRepository<RenewalReminder, UUID> {

    boolean existsBySubscriptionIdAndReminderTypeAndPeriodEndAt(
            UUID subscriptionId, RenewalReminderType reminderType, Instant periodEndAt);
}
