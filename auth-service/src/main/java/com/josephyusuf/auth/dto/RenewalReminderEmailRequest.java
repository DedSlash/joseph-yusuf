package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Requête interne envoyée par subscription-service lors de l'envoi d'un
 * rappel de renouvellement (J-3, J-1) ou de l'email d'expiration (J+0).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewalReminderEmailRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private Plan plan;

    /** Type de rappel : J_MINUS_3, J_MINUS_1 ou EXPIRED. */
    @NotBlank
    private String type;

    @NotNull
    private Instant expiresAt;

    private String couponApplied;

    private boolean couponLifetime;
}
