package com.smartpizza.auth.service;

import com.smartpizza.auth.dto.AuthResponse;
import com.smartpizza.auth.dto.LoginRequest;
import com.smartpizza.auth.dto.RegisterRequest;
import com.smartpizza.auth.entity.User;
import com.smartpizza.auth.enums.Role;
import com.smartpizza.auth.repository.UserRepository;
import com.smartpizza.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    public boolean emailExists(String email) {
        // Email lookup requires a non-empty email value.
        if (email == null || email.trim().isEmpty()) {
            log.warn("Email existence check failed because email is missing");
            throw new RuntimeException("Email is required");
        }

        String normalizedEmail = email.trim();

        log.info("Checking email availability. email={}", normalizedEmail);

        // Trim email before checking to avoid false mismatch due to extra spaces.
        boolean exists = userRepository.existsByEmail(normalizedEmail);

        log.info("Email availability checked. email={}, exists={}", normalizedEmail, exists);

        return exists;
    }

    public AuthResponse register(RegisterRequest request) {

        log.info("User registration started. email={}", request.getEmail());

        // Prevent duplicate registration using the same email.
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User registration failed because email already exists. email={}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        // Default role is CUSTOMER when role is not explicitly provided.
        Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();

        // Create user entity with encoded password before saving.
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(role)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        log.info(
                "User registered successfully. userId={}, email={}, role={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        // Generate JWT after successful registration.
        String token = jwtUtil.generateToken(savedUser);

        log.info("JWT generated after registration. userId={}, role={}", savedUser.getId(), savedUser.getRole());

        // Return authenticated user details with token.
        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt started. email={}", request.getEmail());

        // Delegate credential validation to Spring Security AuthenticationManager.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Fetch user details after successful authentication.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed because authenticated user was not found. email={}", request.getEmail());
                    return new RuntimeException("User not found");
                });

        // Generate JWT for authenticated user.
        String token = jwtUtil.generateToken(user);

        log.info(
                "Login successful. userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        // Return login response with token and basic user profile details.
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Login successful")
                .build();
    }
}