package com.josephyusuf.ruleengine.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "am9zZXBoeXVzdWYtand0LXNlY3JldC1rZXktMjAyNi1kZXYtb25seS1jaGFuZ2UtaW4tcHJvZA==";
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        Field secretField = JwtService.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtService, SECRET);
    }

    private String generateToken(String userId, String plan, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject("user@test.com")
                .claim("userId", userId)
                .claim("plan", plan)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("extractUserId - returns correct userId")
    void extractUserId() {
        String token = generateToken(USER_ID, "FREE", 60000);
        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("extractPlan - returns correct plan")
    void extractPlan() {
        String token = generateToken(USER_ID, "PREMIUM_PLUS", 60000);
        assertThat(jwtService.extractPlan(token)).isEqualTo("PREMIUM_PLUS");
    }

    @Test
    @DisplayName("isTokenValid - valid token")
    void isTokenValid_valid() {
        String token = generateToken(USER_ID, "FREE", 60000);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - expired token")
    void isTokenValid_expired() {
        String token = generateToken(USER_ID, "FREE", -1000);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - malformed token")
    void isTokenValid_malformed() {
        assertThat(jwtService.isTokenValid("garbage.token.here")).isFalse();
    }
}
