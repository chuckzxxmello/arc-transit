package com.transit.arctransit.auth;

import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.mockito.Mockito.*;

class AdminUserInitializerTest {
    @Test
    void existingBootstrapAccountIsNeverPromotedOnRestart() {
        AppUserRepository users = mock(AppUserRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(users.findByUsername("admin")).thenReturn(Optional.of(new AppUser("admin", "hash", "Admin", null)));
        new AdminUserInitializer(users, passwords, jdbc, "admin", "configured-secret").run();
        verifyNoInteractions(jdbc, passwords);
        verify(users, never()).save(any());
    }

    @Test
    void anotherAdministratorPreventsCreationOfBootstrapAccount() {
        AppUserRepository users = mock(AppUserRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM arc.user_roles WHERE role_code = ?)",
                Boolean.class, "SYSTEM_ADMIN")).thenReturn(true);
        new AdminUserInitializer(users, passwords, jdbc, "admin", "configured-secret").run();
        verifyNoInteractions(passwords);
        verify(users, never()).save(any());
    }
}
