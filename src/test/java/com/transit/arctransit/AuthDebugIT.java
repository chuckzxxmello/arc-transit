package com.transit.arctransit;

import com.transit.arctransit.auth.domain.AppUserRepository;
import com.transit.arctransit.auth.security.AppUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class AuthDebugIT {

    @Autowired
    private AppUserDetailsService appUserDetailsService;

    @Test
    public void testAdminUserRoles() {
        UserDetails adminUser = appUserDetailsService.loadUserByUsername("admin");
        System.out.println("TEST DEBUG AUTHORITIES: " + adminUser.getAuthorities());
    }
}
