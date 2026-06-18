package com.smartpizza.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private GatewayAuthFilter gatewayAuthFilter;

	@Test
	void doFilterInternal_whenPublicAuthPath_shouldAllowWithoutToken() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());

		verifyNoInteractions(jwtUtil);
	}

	@Test
	void doFilterInternal_whenPublicMenuGetApi_shouldAllowWithoutToken() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/menu-items");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());

		verifyNoInteractions(jwtUtil);
	}

	@Test
	void doFilterInternal_whenOptionsRequest_shouldAllowWithoutToken() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/cart/1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());

		verifyNoInteractions(jwtUtil);
	}

	@Test
	void doFilterInternal_whenProtectedApiWithoutToken_shouldReturnUnauthorized() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains("Missing or invalid Authorization header"));
		assertNull(filterChain.getRequest());

		verifyNoInteractions(jwtUtil);
	}

	@Test
	void doFilterInternal_whenCustomerAccessCartApi_shouldAllowRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/1");
		request.addHeader("Authorization", "Bearer valid-customer-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("valid-customer-token")).thenReturn("CUSTOMER");
		when(jwtUtil.extractUserId("valid-customer-token")).thenReturn(1L);

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());
		assertEquals(1L, request.getAttribute("userId"));
		assertEquals("CUSTOMER", request.getAttribute("role"));

		verify(jwtUtil).extractRole("valid-customer-token");
		verify(jwtUtil).extractUserId("valid-customer-token");
	}

	@Test
	void doFilterInternal_whenCustomerAccessAdminApi_shouldReturnForbidden() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
		request.addHeader("Authorization", "Bearer valid-customer-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("valid-customer-token")).thenReturn("CUSTOMER");
		when(jwtUtil.extractUserId("valid-customer-token")).thenReturn(1L);

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(403, response.getStatus());
		assertTrue(response.getContentAsString().contains("Access denied for role: CUSTOMER"));
		assertNull(filterChain.getRequest());

		verify(jwtUtil).extractRole("valid-customer-token");
		verify(jwtUtil).extractUserId("valid-customer-token");
	}

	@Test
	void doFilterInternal_whenAdminAccessAdminApi_shouldAllowRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
		request.addHeader("Authorization", "Bearer valid-admin-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("valid-admin-token")).thenReturn("ADMIN");
		when(jwtUtil.extractUserId("valid-admin-token")).thenReturn(99L);

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());
		assertEquals(99L, request.getAttribute("userId"));
		assertEquals("ADMIN", request.getAttribute("role"));

		verify(jwtUtil).extractRole("valid-admin-token");
		verify(jwtUtil).extractUserId("valid-admin-token");
	}

	@Test
	void doFilterInternal_whenDeliveryAccessDeliveryStatusApi_shouldAllowRequest()
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/delivery/status/5");
		request.addHeader("Authorization", "Bearer valid-delivery-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("valid-delivery-token")).thenReturn("DELIVERY");
		when(jwtUtil.extractUserId("valid-delivery-token")).thenReturn(10L);

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());
		assertEquals(10L, request.getAttribute("userId"));
		assertEquals("DELIVERY", request.getAttribute("role"));

		verify(jwtUtil).extractRole("valid-delivery-token");
		verify(jwtUtil).extractUserId("valid-delivery-token");
	}

	@Test
	void doFilterInternal_whenInvalidToken_shouldReturnUnauthorized() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/1");
		request.addHeader("Authorization", "Bearer invalid-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("invalid-token")).thenThrow(new JwtException("Invalid token"));

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains("Invalid or expired JWT token"));
		assertNull(filterChain.getRequest());

		verify(jwtUtil).extractRole("invalid-token");
		verify(jwtUtil, never()).extractUserId("invalid-token");
	}

	@Test
	void doFilterInternal_whenCustomerAccessAiApi_shouldAllowRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ai/recommendations/1");
		request.addHeader("Authorization", "Bearer valid-customer-token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		when(jwtUtil.extractRole("valid-customer-token")).thenReturn("CUSTOMER");
		when(jwtUtil.extractUserId("valid-customer-token")).thenReturn(1L);

		gatewayAuthFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNotNull(filterChain.getRequest());
		assertEquals(1L, request.getAttribute("userId"));
		assertEquals("CUSTOMER", request.getAttribute("role"));

		verify(jwtUtil).extractRole("valid-customer-token");
		verify(jwtUtil).extractUserId("valid-customer-token");
	}
}