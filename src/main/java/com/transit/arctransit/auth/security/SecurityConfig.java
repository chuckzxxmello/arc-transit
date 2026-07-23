package com.transit.arctransit.auth.security;

import com.transit.arctransit.auth.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central Spring Security configuration for Arc Transit.
 *
 * VaadinSecurityConfigurer provides the Vaadin-specific security
 * integration, including navigation access checks and safe handling
 * of Vaadin framework requests.
 * 
 * @PreAuthorize("hasRole('SYSTEM_ADMIN')")
 * 
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        /*
         * Configure Spring Security for Vaadin Flow.
         *
         * Important protections such as CSRF handling remain enabled.
         * The configuration also redirects unauthenticated navigation
         * attempts to LoginView.
         */
        http.with(
                VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}