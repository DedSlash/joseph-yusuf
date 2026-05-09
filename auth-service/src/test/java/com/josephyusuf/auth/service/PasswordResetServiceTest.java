package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.ForgotPasswordRequest;
import com.josephyusuf.auth.dto.ResetPasswordRequest;
import com.josephyusuf.auth.entity.PasswordResetToken;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.InvalidResetTokenException;
import com.josephyusuf.auth.repository.PasswordResetTokenRepository;
import com.josephyusuf.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private PasswordResetService service;

    private static final String EMAIL = "alice@example.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TOKEN_UUID = UUID.randomUUID();

    private User sampleUser() {
        return User.builder().id(USER_ID).email(EMAIL).password("oldHash").build();
    }

    @Nested
    @DisplayName("requestReset")
    class RequestResetTests {

        @Test
        @DisplayName("Email existant : token créé + email envoyé")
        void existingEmail_createsTokenAndSendsEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser()));
            when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

            service.requestReset(ForgotPasswordRequest.builder().email(EMAIL).build());

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            PasswordResetToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getToken()).isNotNull();
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());

            verify(emailService).sendPasswordResetEmail(eq(EMAIL), anyString());
        }

        @Test
        @DisplayName("Email inconnu : aucune action visible (pas d'exception)")
        void unknownEmail_silent() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            service.requestReset(ForgotPasswordRequest.builder().email("unknown@example.com").build());

            verifyNoInteractions(tokenRepository, emailService);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Token valide : mdp mis à jour + token marqué used")
        void validToken_resetsPassword() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(USER_ID).token(TOKEN_UUID)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .used(false).build();
            when(tokenRepository.findByToken(TOKEN_UUID)).thenReturn(Optional.of(token));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser()));
            when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");

            service.resetPassword(ResetPasswordRequest.builder()
                    .token(TOKEN_UUID.toString()).newPassword("newPassword123").build());

            verify(userRepository).save(argThat(u -> "newHash".equals(u.getPassword())));
            verify(tokenRepository).save(argThat(PasswordResetToken::isUsed));
        }

        @Test
        @DisplayName("Token déjà utilisé : InvalidResetTokenException")
        void usedToken_throws() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(USER_ID).token(TOKEN_UUID)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .used(true).build();
            when(tokenRepository.findByToken(TOKEN_UUID)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                    .token(TOKEN_UUID.toString()).newPassword("newPassword123").build()))
                    .isInstanceOf(InvalidResetTokenException.class)
                    .hasMessageContaining("utilis");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Token expiré : InvalidResetTokenException")
        void expiredToken_throws() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(USER_ID).token(TOKEN_UUID)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .used(false).build();
            when(tokenRepository.findByToken(TOKEN_UUID)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                    .token(TOKEN_UUID.toString()).newPassword("newPassword123").build()))
                    .isInstanceOf(InvalidResetTokenException.class)
                    .hasMessageContaining("expir");
        }

        @Test
        @DisplayName("Token introuvable : InvalidResetTokenException")
        void unknownToken_throws() {
            when(tokenRepository.findByToken(TOKEN_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                    .token(TOKEN_UUID.toString()).newPassword("newPassword123").build()))
                    .isInstanceOf(InvalidResetTokenException.class)
                    .hasMessageContaining("invalide");
        }

        @Test
        @DisplayName("Token au format invalide : InvalidResetTokenException")
        void malformedToken_throws() {
            assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                    .token("not-a-uuid").newPassword("newPassword123").build()))
                    .isInstanceOf(InvalidResetTokenException.class)
                    .hasMessageContaining("Format");
        }

        @Test
        @DisplayName("Token valide mais utilisateur supprimé : InvalidResetTokenException")
        void userMissing_throws() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(USER_ID).token(TOKEN_UUID)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .used(false).build();
            when(tokenRepository.findByToken(TOKEN_UUID)).thenReturn(Optional.of(token));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                    .token(TOKEN_UUID.toString()).newPassword("newPassword123").build()))
                    .isInstanceOf(InvalidResetTokenException.class);
        }
    }
}
