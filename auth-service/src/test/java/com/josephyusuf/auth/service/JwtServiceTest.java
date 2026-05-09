package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private static final String TEST_SECRET_KEY = "am9zZXBoeXVzdWYtand0LXNlY3JldC1rZXktMjAyNi1kZXYtb25seS1jaGFuZ2UtaW4tcHJvZA==";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L;

    private UUID userId;
    private String email;
    private Plan plan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);

        userId = UUID.randomUUID();
        email = "test@example.com";
        plan = Plan.FREE;
    }

    @Test
    @DisplayName("generateAccessToken - creates valid parseable token")
    void generateAccessToken_createsValidToken() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("extractEmail - returns correct email from token")
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("extractUserId - returns correct userId from token")
    void extractUserId_returnsCorrectUserId() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        String extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("extractPlan - returns correct plan from token")
    void extractPlan_returnsCorrectPlan() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        String extractedPlan = jwtService.extractPlan(token);

        assertThat(extractedPlan).isEqualTo(Plan.FREE.name());
    }

    @Test
    @DisplayName("isTokenValid - returns true for valid token")
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - returns false for expired token")
    void isTokenValid_returnsFalseForExpiredToken() {
        // Set a negative expiration to generate an already-expired token
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);

        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for tampered token")
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);
        String tamperedToken = token + "tampered";

        boolean isValid = jwtService.isTokenValid(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for null-like invalid token")
    void isTokenValid_returnsFalseForInvalidToken() {
        boolean isValid = jwtService.isTokenValid("invalid.token.value");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("generateAccessToken - token contains correct claims for PREMIUM plan")
    void generateAccessToken_correctClaimsForPremiumPlan() {
        Plan premiumPlan = Plan.PREMIUM;
        String token = jwtService.generateAccessToken(userId, email, premiumPlan, Role.ADMIN);

        assertThat(jwtService.extractPlan(token)).isEqualTo("PREMIUM");
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId.toString());
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractRole - returns correct role from token")
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateAccessToken(userId, email, plan, Role.USER);

        String extractedRole = jwtService.extractRole(token);

        assertThat(extractedRole).isEqualTo(Role.USER.name());
    }
}
