package com.smartpizza.gateway.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        // Build HMAC signing key from configured JWT secret.(HMAC-SHA Algorithm)
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        // Validate token signature and extract JWT payload claims.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractRole(String token) {
        // Extract user role from token claims for gateway authorization.
        return extractClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        // Extract authenticated user id from token claims.
        Object userId = extractClaims(token).get("userId");

        // JWT numeric claims may be deserialized as Integer depending on value size.
        if (userId instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        // If claim is already Long, return directly.
        if (userId instanceof Long longValue) {
            return longValue;
        }

        // Fallback for String/other numeric representation.
        return Long.valueOf(userId.toString());
    }
}