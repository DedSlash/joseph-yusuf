package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.RenewalReminderType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Requête envoyée à auth-service pour déclencher l'envoi d'un email de
 * rappel de renouvellement. Le mapping plan ⇄ Plan auth-service est par
 * nom (PREMIUM, PREMIUM_PLUS, FREE — enums alignés).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewalReminderEmailRequest {

    private UUID userId;
    private PlanTier plan;
    private RenewalReminderType type;
    private Instant expiresAt;
    private String couponApplied;
    private boolean couponLifetime;
}
