package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.RefreshToken;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.TokenExpiredException;
import com.josephyusuf.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Joseph")
                .lastName("Yusuf")
                .plan(Plan.FREE)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("createRefreshToken - creates and saves a refresh token")
    void createRefreshToken_nominal() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getToken()).isNotNull().isNotEmpty();
        assertThat(result.getExpiryDate()).isAfter(Instant.now());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getExpiryDate()).isBetween(
                Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION - 5000),
                Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION + 5000)
        );
    }

    @Test
    @DisplayName("verifyRefreshToken - returns token when valid and not expired")
    void verifyRefreshToken_valid() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION))
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken("valid-token");

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("valid-token");
        assertThat(result.getUser()).isEqualTo(testUser);

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyRefreshToken - throws TokenExpiredException when token not found")
    void verifyRefreshToken_notFound() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("unknown-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessage("Refresh token invalide");
    }

    @Test
    @DisplayName("verifyRefreshToken - throws TokenExpiredException when token is expired")
    void verifyRefreshToken_expired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("expired-token")
                .user(testUser)
                .expiryDate(Instant.now().minusMillis(1000)) // expired 1 second ago
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("expired-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessage("Refresh token expiré");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("deleteByToken - calls repository deleteByToken")
    void deleteByToken_nominal() {
        refreshTokenService.deleteByToken("token-to-delete");

        verify(refreshTokenRepository).deleteByToken("token-to-delete");
    }
}
