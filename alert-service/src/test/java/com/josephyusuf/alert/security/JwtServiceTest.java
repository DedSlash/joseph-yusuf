package com.josephyusuf.alert.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "am9zZXBoeXVzdWYtand0LXNlY3JldC1rZXktMjAyNi1kZXYtb25seS1jaGFuZ2UtaW4tcHJvZA==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
    }

    private String generateToken(String userId, String plan, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Date now = new Date();
        return Jwts.builder()
                .subject("user@example.com")
                .claims(Map.of("userId", userId, "plan", plan))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    @Test
    void extractUserId_returnsClaim() {
        String userId = UUID.randomUUID().toString();
        String token = generateToken(userId, "PREMIUM", 60_000);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractPlan_returnsClaim() {
        String token = generateToken(UUID.randomUUID().toString(), "PREMIUM_PLUS", 60_000);

        assertThat(jwtService.extractPlan(token)).isEqualTo("PREMIUM_PLUS");
    }

    @Test
    void extractEmail_returnsSubject() {
        String token = generateToken(UUID.randomUUID().toString(), "FREE", 60_000);

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = generateToken(UUID.randomUUID().toString(), "FREE", 60_000);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        String token = generateToken(UUID.randomUUID().toString(), "FREE", -1_000);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForGarbageToken() {
        assertThat(jwtService.isTokenValid("not-a-token")).isFalse();
    }
}
