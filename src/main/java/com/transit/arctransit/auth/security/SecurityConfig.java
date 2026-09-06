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

// central Spring Security configuration.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // public static resources
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
								               			"/images/**",
								                        "/line-awesome/**"
								               )
								               .permitAll()
        );
        
        http.with(
                VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class)
        );

        // Security headers
        http.headers(headers -> 
        					headers.frameOptions(frame -> frame.sameOrigin())
					               .contentTypeOptions(
					                        org.springframework.security.config.Customizer
					                        .withDefaults()
					               )
        );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}