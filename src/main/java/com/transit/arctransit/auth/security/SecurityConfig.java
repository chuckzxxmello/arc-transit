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
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        /*
         * Configure Spring Security for Vaadin Flow.
         */
        http.with(
                VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class));

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/images/**").permitAll()
            .requestMatchers("/line-awesome/**").permitAll()
            .requestMatchers("/**").permitAll()
        );

        http.requestCache(cache -> cache
                .requestCache(new com.vaadin.flow.spring.security.VaadinDefaultRequestCache())
        );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}