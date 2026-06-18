package com.smartpizza.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.smartpizza.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration}")
	private Long jwtExpiration;

	private SecretKey getSigningKey() {
		// Build HMAC signing key from configured JWT secret.
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(User user) {
		// Generate JWT with user identity, role claims and configured expiry time.
		return Jwts.builder().subject(user.getEmail())

				// Custom claims used by gateway/frontend for identity and role-based access.
				.claim("userId", user.getId()).claim("role", user.getRole().name())
				.claim("fullName", user.getFullName())

				// Token metadata: issue time and expiry time.
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + jwtExpiration))

				// Sign token using configured HMAC secret key.
				.signWith(getSigningKey()).compact();
	}

	public Claims extractClaims(String token) {
		// Verify token signature and extract claims from token payload.
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}

	public String extractEmail(String token) {
		// Email is stored as JWT subject during token generation.
		return extractClaims(token).getSubject();
	}
}