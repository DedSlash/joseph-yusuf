package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bundle des claims utilisés pour générer un JWT access token. Regroupe
 * tous les paramètres utilisateur en un seul objet pour simplifier la
 * signature {@link com.josephyusuf.auth.service.JwtService#generateAccessToken}.
 */
@Getter
@Builder
public class AccessTokenClaims {

    private final UUID userId;
    private final String email;
    private final Plan plan;
    private final Role role;
    private final String country;
    private final String currency;
    private final boolean inTrial;
    private final LocalDateTime trialEndsAt;
}
