package com.smartpizza.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

	private JwtUtil jwtUtil;

	private final String jwtSecret = "smartpizza-super-secure-jwt-secret-key-2026";

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil();

		// Inject test secret into private @Value field.
		ReflectionTestUtils.setField(jwtUtil, "jwtSecret", jwtSecret);
	}

	@Test
	void extractClaims_whenTokenIsValid_shouldReturnClaims() {
		String token = generateToken(1L, "CUSTOMER", "customer@gmail.com");

		Claims claims = jwtUtil.extractClaims(token);

		assertNotNull(claims);
		assertEquals("customer@gmail.com", claims.getSubject());
		assertEquals("CUSTOMER", claims.get("role", String.class));
		assertEquals(1, claims.get("userId"));
	}

	@Test
	void extractRole_whenTokenIsValid_shouldReturnRole() {
		String token = generateToken(10L, "ADMIN", "admin@gmail.com");

		String role = jwtUtil.extractRole(token);

		assertEquals("ADMIN", role);
	}

	@Test
	void extractUserId_whenUserIdClaimIsInteger_shouldReturnLongValue() {
		String token = Jwts.builder().subject("customer@gmail.com").claim("userId", 5).claim("role", "CUSTOMER")
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600000))
				.signWith(getSigningKey()).compact();

		Long userId = jwtUtil.extractUserId(token);

		assertEquals(5L, userId);
	}

	@Test
	void extractUserId_whenUserIdClaimIsLong_shouldReturnLongValue() {
		String token = generateToken(25L, "DELIVERY", "delivery@gmail.com");

		Long userId = jwtUtil.extractUserId(token);

		assertEquals(25L, userId);
	}

	@Test
	void extractUserId_whenUserIdClaimIsString_shouldReturnLongValue() {
		String token = Jwts.builder().subject("customer@gmail.com").claim("userId", "99").claim("role", "CUSTOMER")
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600000))
				.signWith(getSigningKey()).compact();

		Long userId = jwtUtil.extractUserId(token);

		assertEquals(99L, userId);
	}

	@Test
	void extractClaims_whenTokenIsInvalid_shouldThrowJwtException() {
		String invalidToken = "invalid.jwt.token";

		assertThrows(JwtException.class, () -> jwtUtil.extractClaims(invalidToken));
	}

	@Test
	void extractClaims_whenTokenIsSignedWithDifferentSecret_shouldThrowJwtException() {
		String differentSecret = "different-super-secure-jwt-secret-key-2026";

		SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

		String token = Jwts.builder().subject("customer@gmail.com").claim("userId", 1L).claim("role", "CUSTOMER")
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600000)).signWith(differentKey)
				.compact();

		assertThrows(JwtException.class, () -> jwtUtil.extractClaims(token));
	}

	private String generateToken(Long userId, String role, String email) {
		return Jwts.builder().subject(email).claim("userId", userId).claim("role", role).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3600000)).signWith(getSigningKey()).compact();
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
}