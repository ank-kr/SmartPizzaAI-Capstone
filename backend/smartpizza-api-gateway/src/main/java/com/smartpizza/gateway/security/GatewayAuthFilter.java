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

        if ("OPTIONS".equalsIgnoreCase(method) || isPublicPath(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractUserId(token);

            if (!isAccessAllowed(path, method, role)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied for role: " + role);
                return;
            }

            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    private boolean isPublicPath(String path, String method) {
        if (path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/health")
                || path.startsWith("/ai/health")) {
            return true;
        }

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

        if (path.startsWith("/api/admin/")) {
            return hasAnyRole(role, "ADMIN");
        }

        if (path.startsWith("/analytics/")) {
            return hasAnyRole(role, "ADMIN");
        }

        if (path.startsWith("/api/cart/")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        if (path.startsWith("/api/orders/place")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        if (path.startsWith("/api/orders/admin/")) {
            return hasAnyRole(role, "ADMIN");
        }

        if (path.startsWith("/api/orders/user/")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        if (path.matches("^/api/orders/\\d+$")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        if (path.startsWith("/api/payments/")) {
            return hasAnyRole(role, "CUSTOMER");
        }

        if (path.startsWith("/api/delivery/partners") && "POST".equalsIgnoreCase(method)) {
            return hasAnyRole(role, "ADMIN");
        }

        if (path.startsWith("/api/delivery/assign/")) {
            return hasAnyRole(role, "ADMIN");
        }

        if (path.startsWith("/api/delivery/status/")) {
            return hasAnyRole(role, "DELIVERY", "ADMIN");
        }

        if (path.startsWith("/api/delivery/")) {
            return hasAnyRole(role, "CUSTOMER", "DELIVERY", "ADMIN");
        }

        if (path.startsWith("/ai/")) {
            return hasAnyRole(role, "CUSTOMER", "ADMIN");
        }

        return true;
    }

    private boolean hasAnyRole(String actualRole, String... allowedRoles) {
        Set<String> allowedRoleSet = Set.of(allowedRoles);
        return allowedRoleSet.contains(actualRole);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
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