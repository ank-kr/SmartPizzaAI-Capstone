package com.smartpizza.gateway.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow CORS preflight requests and public endpoints without JWT validation.
        if ("OPTIONS".equalsIgnoreCase(method) || isPublicPath(path, method)) {
            log.debug("Public/preflight request allowed. path={}, method={}", path, method);
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        // Protected APIs must contain a Bearer token in the Authorization header.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header. path={}, method={}", path, method);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            // Extract authenticated user details from JWT.
            String role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractUserId(token);

            // Enforce role-based access control at API Gateway level.
            if (!isAccessAllowed(path, method, role)) {
                log.warn("Access denied by gateway. path={}, method={}, role={}", path, method, role);
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied for role: " + role);
                return;
            }

            // Attach authenticated user details for downstream services if needed.
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            log.debug("Gateway access allowed. path={}, method={}, userId={}, role={}", path, method, userId, role);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException exception) {
            // Reject expired, malformed, or invalid JWT tokens.
            log.warn("Invalid or expired JWT token. path={}, method={}", path, method);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    private boolean isPublicPath(String path, String method) {
        // Auth and health-check endpoints are accessible without authentication.
        if (path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/health")
                || path.startsWith("/auth/check-email")
                || path.startsWith("/ai/health")) {
            return true;
        }

        // Public GET APIs used for menu/category browsing.
        if ("GET".equalsIgnoreCase(method)
                && (path.startsWith("/api/health")
                || path.startsWith("/api/categories")
                || path.startsWith("/api/menu-items"))) {
            return true;
        }

        return false;
    }

    private boolean isAccessAllowed(String path, String method, String role) {
        if (role == null) {
            return false;
        }

        // Admin-only APIs.
        if (path.startsWith("/api/admin/")) {
            return hasAnyRole(role, "ADMIN");
        }

        // Analytics dashboard is restricted to admin users.
        if (path.startsWith("/analytics/")) {
            return hasAnyRole(role, "ADMIN");
        }

        // Cart operations are restricted to customers.
        if (path.startsWith("/api/cart/")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        // Only customers can place orders.
        if (path.startsWith("/api/orders/place")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        // Admin order management APIs.
        if (path.startsWith("/api/orders/admin/")) {
            return hasAnyRole(role, "ADMIN");
        }

        // Customer can view own orders; admin can view user orders for support/admin use.
        if (path.startsWith("/api/orders/user/")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        // Fetching a specific order is allowed for customer/admin roles.
        if (path.matches("^/api/orders/\\d+$")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        // Payment APIs are restricted to customers.
        if (path.startsWith("/api/payments/")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        // Creating delivery partner profiles is an admin operation.
        if (path.startsWith("/api/delivery/partners") && "POST".equalsIgnoreCase(method)) {
            return hasAnyRole(role, "ADMIN");
        }

        // Manual delivery assignment is admin-only.
        if (path.startsWith("/api/delivery/assign/")) {
            return hasAnyRole(role, "ADMIN");
        }

        // Delivery status can be updated by delivery partner or admin.
        if (path.startsWith("/api/delivery/status/")) {
            return hasAnyRole(role, "DELIVERY", "ADMIN");
        }

        // Delivery tracking/details can be accessed by customer, delivery partner, or admin.
        if (path.startsWith("/api/delivery/")) {
            return hasAnyRole(role, "CUSTOMER", "DELIVERY", "ADMIN");
        }

        // AI recommendation APIs are available to customer and admin users.
        if (path.startsWith("/ai/")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        // Default allow for unmatched routes; specific restrictions are handled above.
        return true;
    }

    private boolean hasAnyRole(String actualRole, String... allowedRoles) {
        // Check whether authenticated user's role exists in allowed role list.
        Set<String> allowedRoleSet = Set.of(allowedRoles);
        return allowedRoleSet.contains(actualRole);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        // Send consistent JSON error response from gateway filter.
        response.setStatus(status);
        response.setContentType("application/json");

        String responseBody = """
                {
                  "status": %d,
                  "message": "%s"
                }
                """.formatted(status, message);

        response.getWriter().write(responseBody);
    }
}