package com.transit.arctransit.auth;

import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;
import com.transit.arctransit.auth.domain.UserRole;
import com.transit.arctransit.auth.domain.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the default system administrator if none exist.
 * Reads credentials securely from application.properties to avoid hardcoding in SQL.
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${arc.security.admin.username:admin}")
    private String adminUsername;

    @Value("${arc.security.admin.password:ArcTransit123}")
    private String adminPassword;

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public AdminUserInitializer(AppUserRepository userRepository, UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            AppUser admin = new AppUser(adminUsername, passwordEncoder.encode(adminPassword), "Default Admin", null);
            admin = userRepository.save(admin);
            
            jdbcTemplate.update("INSERT INTO arc.user_roles (user_id, role_code) VALUES (?, ?)", admin.getId(), "SYSTEM_ADMIN");

            System.out.println("Default System Administrator initialized via JdbcTemplate.");
        } else {
            // Check if admin user exists but is missing the role (due to old database state)
            userRepository.findByUsername(adminUsername).ifPresent(admin -> {
                if (admin.getUserRoles().isEmpty()) {
                    jdbcTemplate.update("INSERT INTO arc.user_roles (user_id, role_code) VALUES (?, ?)", admin.getId(), "SYSTEM_ADMIN");
                    System.out.println("Added SYSTEM_ADMIN role to existing " + adminUsername + " user via JdbcTemplate.");
                }
            });
        }
    }
}
