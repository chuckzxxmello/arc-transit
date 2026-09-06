package com.transit.arctransit;

import com.transit.arctransit.auth.security.AppUserDetailsService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertTrue;



// integration test for database-backed authentication
@SpringBootTest
class AuthenticationIntegrationTest {

    @Autowired
    private AppUserDetailsService appUserDetailsService;

    @Test
    void adminUserHasSystemAdminRole() {

        UserDetails adminUser = appUserDetailsService.loadUserByUsername("admin");

        assertTrue(
                adminUser.getAuthorities()
                        .contains(
                                new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")
                        ),
                "admin user should have ROLE_SYSTEM_ADMIN"
        );
    }
}