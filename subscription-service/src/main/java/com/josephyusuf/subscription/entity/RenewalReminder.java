package com.josephyusuf.subscription.entity;

import com.josephyusuf.subscription.enums.RenewalReminderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'un rappel envoyé pour une subscription donnée. La contrainte unique
 * (subscription_id, reminder_type, period_end_at) garantit qu'on n'envoie
 * pas deux fois le même rappel pour la même période, même si le cron tourne
 * plusieurs fois dans la journée.
 */
@Entity
@Table(name = "renewal_reminders", schema = "joseph_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewalReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 20)
    private RenewalReminderType reminderType;

    @Column(name = "period_end_at", nullable = false)
    private Instant periodEndAt;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = Instant.now();
    }
}
