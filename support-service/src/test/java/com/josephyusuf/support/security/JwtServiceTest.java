package com.josephyusuf.support.security;

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

    private static final String SECRET =
            "am9zZXBoeXVzdWYtand0LXNlY3JldC1rZXktMjAyNi1kZXYtb25seS1jaGFuZ2UtaW4tcHJvZA==";

    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private String buildToken(Map<String, Object> claims, long ttlMillis) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void extractUserId_returnsClaim() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(Map.of("userId", userId, "plan", "PREMIUM", "role", "USER"), 60_000);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractPlan_returnsClaim() {
        String token = buildToken(Map.of("userId", "u", "plan", "FREE"), 60_000);

        assertThat(jwtService.extractPlan(token)).isEqualTo("FREE");
    }

    @Test
    void extractRole_returnsClaim() {
        String token = buildToken(Map.of("userId", "u", "role", "ADMIN"), 60_000);

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractEmail_returnsClaim() {
        String token = buildToken(Map.of("userId", "u", "email", "x@y.com"), 60_000);

        assertThat(jwtService.extractEmail(token)).isEqualTo("x@y.com");
    }

    @Test
    void extractEmail_returnsNull_whenClaimAbsent() {
        String token = buildToken(Map.of("userId", "u"), 60_000);

        assertThat(jwtService.extractEmail(token)).isNull();
    }

    @Test
    void isTokenValid_returnsTrue_forFreshToken() {
        String token = buildToken(Map.of("userId", "u"), 60_000);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        String token = buildToken(Map.of("userId", "u"), -1000);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forTokenWithWrongSignature() {
        SecretKey other = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbm90LXJlYWwtMTIzNDU="));
        String token = Jwts.builder()
                .claim("userId", "u")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(other)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
