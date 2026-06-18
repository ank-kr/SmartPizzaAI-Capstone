package com.smartpizza.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smartpizza.auth.repository.UserRepository;
import com.smartpizza.auth.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration // spring scans this class and register/execute all method with @bean annotation
@RequiredArgsConstructor // LOMBOK annotation (generate constructor for all final fields).
public class SecurityConfig {

	private final UserRepository userRepository; // fetch users from DB

	@Bean // creates and register this object as a spring bean so spring can manage and
			// inject wherever needs(Inversion of Control)
	public UserDetailsService userDetailsService() {
		return new CustomUserDetailsService(userRepository); // return, creates an object of customerdetailservice and
																// store this object in its container.
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		// DaoAuthenticationProvider uses UserDetailsService and PasswordEncoder
		// to authenticate users from the database.
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

		// Loads user details like email, password, role and active status.
		provider.setUserDetailsService(userDetailsService());

		// Uses BCrypt to compare raw login password with encoded DB password.
		provider.setPasswordEncoder(passwordEncoder());

		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		// Exposes Spring Security's AuthenticationManager as a bean
		// so AuthService can use it during login.
		return configuration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// BCrypt hashes passwords before saving and verifies them during login.
		return new BCryptPasswordEncoder();
	}

	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                // Disable CSRF because this service uses stateless JWT-based APIs.
                .csrf(csrf -> csrf.disable())

                // Enable CORS support so frontend/API Gateway can call auth APIs.
                .cors(cors -> {})

                // Stateless session: server will not store login session.
                // Each protected request must carry JWT token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints are accessible without JWT.
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/health",
                                "/auth/check-email"
                        ).permitAll()

                        // Any other endpoint requires authentication.
                        .anyRequest().authenticated()
                );

        return httpSecurity.build();
    }
}