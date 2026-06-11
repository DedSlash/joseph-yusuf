package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.AccessTokenClaims;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(UUID userId, String email, Plan plan, Role role,
                                       String country, String currency) {
        return generateAccessToken(AccessTokenClaims.builder()
                .userId(userId).email(email).plan(plan).role(role)
                .country(country).currency(currency)
                .inTrial(false).trialEndsAt(null)
                .build());
    }

    public String generateAccessToken(AccessTokenClaims claims) {
        var builder = Jwts.builder()
                .subject(claims.getEmail())
                .claim("userId", claims.getUserId().toString())
                .claim("plan", claims.getPlan().name())
                .claim("role", claims.getRole().name())
                .claim("country", claims.getCountry() != null ? claims.getCountry() : "SN")
                .claim("currency", claims.getCurrency() != null ? claims.getCurrency() : "XOF")
                .claim("inTrial", claims.isInTrial());

        if (claims.getTrialEndsAt() != null) {
            builder.claim("trialEndsAt", claims.getTrialEndsAt().toString());
        }

        return builder
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractPlan(String token) {
        return extractClaim(token, claims -> claims.get("plan", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractCountry(String token) {
        return extractClaim(token, claims -> claims.get("country", String.class));
    }

    public String extractCurrency(String token) {
        return extractClaim(token, claims -> claims.get("currency", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
