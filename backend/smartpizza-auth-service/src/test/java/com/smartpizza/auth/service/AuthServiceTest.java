package com.smartpizza.auth.service;

import com.smartpizza.auth.dto.AuthResponse;
import com.smartpizza.auth.dto.LoginRequest;
import com.smartpizza.auth.dto.RegisterRequest;
import com.smartpizza.auth.entity.User;
import com.smartpizza.auth.enums.Role;
import com.smartpizza.auth.repository.UserRepository;
import com.smartpizza.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void emailExists_ShouldReturnTrue_WhenEmailExists() {
        String email = "ankit@gmail.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        boolean result = authService.emailExists(email);

        assertTrue(result);

        verify(userRepository, times(1)).existsByEmail(email);
    }

    @Test
    void emailExists_ShouldTrimEmailBeforeChecking() {
        String email = "  ankit@gmail.com  ";

        when(userRepository.existsByEmail("ankit@gmail.com")).thenReturn(true);

        boolean result = authService.emailExists(email);

        assertTrue(result);

        verify(userRepository, times(1)).existsByEmail("ankit@gmail.com");
    }

    @Test
    void emailExists_ShouldThrowException_WhenEmailIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.emailExists(null)
        );

        assertEquals("Email is required", exception.getMessage());

        verifyNoInteractions(userRepository);
    }

    @Test
    void emailExists_ShouldThrowException_WhenEmailIsBlank() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.emailExists("   ")
        );

        assertEquals("Email is required", exception.getMessage());

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_ShouldRegisterUserSuccessfully_WithProvidedRole() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Ankit Kumar")
                .email("ankit@gmail.com")
                .password("ankit123")
                .phone("9876543210")
                .address("Electronic City, Bengaluru")
                .role(Role.CUSTOMER)
                .build();

        User savedUser = User.builder()
                .id(1L)
                .fullName("Ankit Kumar")
                .email("ankit@gmail.com")
                .password("encodedPassword")
                .phone("9876543210")
                .address("Electronic City, Bengaluru")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userRepository.existsByEmail("ankit@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("ankit123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Ankit Kumar", response.getFullName());
        assertEquals("ankit@gmail.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("Registration successful", response.getMessage());

        verify(userRepository, times(1)).existsByEmail("ankit@gmail.com");
        verify(passwordEncoder, times(1)).encode("ankit123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(savedUser);
    }

    @Test
    void register_ShouldUseCustomerRole_WhenRoleIsNull() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Customer User")
                .email("customer@gmail.com")
                .password("customer123")
                .phone("9999999999")
                .address("Bengaluru")
                .role(null)
                .build();

        User savedUser = User.builder()
                .id(2L)
                .fullName("Customer User")
                .email("customer@gmail.com")
                .password("encodedPassword")
                .phone("9999999999")
                .address("Bengaluru")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userRepository.existsByEmail("customer@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("customer123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser)).thenReturn("customer-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("customer-token", response.getToken());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyRegistered() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Ankit Kumar")
                .email("ankit@gmail.com")
                .password("ankit123")
                .phone("9876543210")
                .address("Electronic City, Bengaluru")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmail("ankit@gmail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository, times(1)).existsByEmail("ankit@gmail.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("ankit@gmail.com")
                .password("ankit123")
                .build();

        User user = User.builder()
                .id(1L)
                .fullName("Ankit Kumar")
                .email("ankit@gmail.com")
                .password("encodedPassword")
                .phone("9876543210")
                .address("Electronic City, Bengaluru")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userRepository.findByEmail("ankit@gmail.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Ankit Kumar", response.getFullName());
        assertEquals("ankit@gmail.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("Login successful", response.getMessage());

        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
        verify(userRepository, times(1)).findByEmail("ankit@gmail.com");
        verify(jwtUtil, times(1)).generateToken(user);
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundAfterAuthentication() {
        LoginRequest request = LoginRequest.builder()
                .email("missing@gmail.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("missing@gmail.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("User not found", exception.getMessage());

        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
        verify(userRepository, times(1)).findByEmail("missing@gmail.com");
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldNotGenerateToken_WhenAuthenticationFails() {
        LoginRequest request = LoginRequest.builder()
                .email("ankit@gmail.com")
                .password("wrongpassword")
                .build();

        doThrow(new RuntimeException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Bad credentials", exception.getMessage());

        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }
}