package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.ForgotPasswordRequest;
import com.josephyusuf.auth.dto.ResetPasswordRequest;
import com.josephyusuf.auth.entity.PasswordResetToken;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.InvalidResetTokenException;
import com.josephyusuf.auth.repository.PasswordResetTokenRepository;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Demande de reset. Toujours silencieux côté API (200) pour ne pas révéler
     * l'existence ou non d'un email.
     */
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(UUID.randomUUID())
                    .expiresAt(Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                    .used(false)
                    .build();
            tokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), token.getToken().toString());
            log.info("Reset token créé userId={}", user.getId());
        }, () -> log.info("Reset demandé pour email inconnu — aucune action visible"));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID tokenUuid = parseToken(request.getToken());
        PasswordResetToken token = tokenRepository.findByToken(tokenUuid)
                .orElseThrow(() -> new InvalidResetTokenException("Token de reset invalide"));

        if (token.isUsed()) {
            throw new InvalidResetTokenException("Ce token a déjà été utilisé");
        }
        if (token.isExpired()) {
            throw new InvalidResetTokenException("Ce token a expiré (validité 15 minutes)");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidResetTokenException("Utilisateur introuvable pour ce token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Mot de passe réinitialisé userId={}", user.getId());
    }

    private UUID parseToken(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidResetTokenException("Format de token invalide");
        }
    }
}
