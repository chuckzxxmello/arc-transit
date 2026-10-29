package com.transit.arctransit;

import com.transit.arctransit.auth.*;
import com.transit.arctransit.common.exception.BusinessConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class UserEditingIntegrationTest {
    @Autowired UserAdministrationService users;
    @Autowired UserDetailsService authenticationUsers;
    @Autowired JdbcTemplate jdbc;
    @Value("${arc.security.admin.username}") String administrator;

    @BeforeEach
    void authenticate() {
        authenticateAs(administrator);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileAndRoleEditPreservesIdentityAndPasswordAndChangesCurrentLogin() {
        UserView original = createStaff();
        String hash = passwordHash(original.id());
        String renamed = name();
        String email = name() + "@example.com";
        UserView updated = users.updateUser(new UpdateUserCommand(original.id(), original.version(),
                renamed.toUpperCase(java.util.Locale.ROOT), " Updated Staff ", email.toUpperCase(java.util.Locale.ROOT), Set.of("SYSTEM_ADMIN")));
        assertEquals(original.id(), updated.id());
        assertEquals(renamed, updated.username());
        assertEquals("Updated Staff", updated.displayName());
        assertEquals(email, updated.email());
        assertEquals(Set.of("SYSTEM_ADMIN"), updated.roles());
        assertEquals(hash, passwordHash(original.id()));
        assertThrows(UsernameNotFoundException.class, () -> authenticationUsers.loadUserByUsername(original.username()));
        assertEquals(renamed, authenticationUsers.loadUserByUsername(renamed).getUsername());
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM arc.username_reservations WHERE user_id=?",
                Long.class, original.id()));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name='USER_UPDATED' AND entity_id=?",
                Long.class, original.id()));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name='USER_ROLES_CHANGED' AND entity_id=?",
                Long.class, original.id()));
    }

    @Test
    void optionalEmailCanBeClearedWithoutReassigningUnchangedRoles() {
        UserView original = createStaff();
        UserView updated = users.updateUser(new UpdateUserCommand(original.id(), original.version(), original.username(),
                original.displayName(), null, original.roles()));
        assertNull(updated.email());
        assertEquals(original.roles(), updated.roles());
        assertTrue(updated.version() > original.version());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name='USER_ROLES_CHANGED' AND entity_id=?",
                Long.class, original.id()));
    }

    @Test
    void currentAndHistoricalUsernameCollisionsDoNotPartiallyEditTheAccount() {
        UserView first = createStaff();
        UserView renamed = users.updateUser(new UpdateUserCommand(first.id(), first.version(), name(),
                first.displayName(), first.email(), first.roles()));
        UserView second = createStaff();
        for (String taken : List.of(first.username(), renamed.username())) {
            assertThrows(BusinessConflictException.class, () -> users.updateUser(new UpdateUserCommand(
                    second.id(), second.version(), taken, "Should not change", null, Set.of("SYSTEM_ADMIN"))));
        }
        UserView unchanged = users.getUser(second.username());
        assertEquals(second, unchanged);
        assertThrows(BusinessConflictException.class, () -> users.createUser(new CreateUserCommand(
                first.username(), "Transit899!@#", "Another Staff", null, Set.of("OPERATIONS_STAFF"))));
        UserView restored = users.updateUser(new UpdateUserCommand(renamed.id(), renamed.version(), first.username(),
                renamed.displayName(), renamed.email(), renamed.roles()));
        assertEquals(first.id(), restored.id());
    }

    @Test
    void existingRoleUpdateCanReplaceOverlappingRolesAndInvalidatesAnOlderEditor() {
        UserView original = createStaff();
        UserView combined = users.replaceRoles(new ReplaceUserRolesCommand(original.username(),
                Set.of("SYSTEM_ADMIN", "OPERATIONS_STAFF")));
        assertEquals(Set.of("SYSTEM_ADMIN", "OPERATIONS_STAFF"), combined.roles());
        assertTrue(combined.version() > original.version());
        assertThrows(BusinessConflictException.class, () -> users.updateUser(new UpdateUserCommand(
                original.id(), original.version(), original.username(), "Stale", null, original.roles())));
        UserView demoted = users.replaceRoles(new ReplaceUserRolesCommand(original.username(), Set.of("OPERATIONS_STAFF")));
        assertEquals(Set.of("OPERATIONS_STAFF"), demoted.roles());
        assertEquals(original.id(), demoted.id());
        assertEquals(original.email(), demoted.email());
    }

    @Test
    void duplicateEmailDoesNotPartiallyChangeTheProfileOrRole() {
        UserView owner = createStaff();
        UserView target = createStaff();
        assertTrue(assertThrows(BusinessConflictException.class, () -> users.updateUser(new UpdateUserCommand(
                target.id(), target.version(), target.username(), "Should not change",
                owner.email().toUpperCase(java.util.Locale.ROOT), Set.of("SYSTEM_ADMIN"))))
                .getMessage().contains("Email address"));
        assertEquals(target, users.getUser(target.username()));
    }

    @Test
    void staleEditorAndArchivedAccountCannotOverwriteCurrentDetails() {
        UserView original = createStaff();
        users.updateUser(new UpdateUserCommand(original.id(), original.version(), original.username(), "First editor",
                original.email(), original.roles()));
        assertTrue(assertThrows(BusinessConflictException.class, () -> users.updateUser(new UpdateUserCommand(
                original.id(), original.version(), original.username(), "Stale editor", null, Set.of("SYSTEM_ADMIN"))))
                .getMessage().contains("another user"));
        assertEquals("First editor", users.getUser(original.username()).displayName());
        users.deleteUser(original.username());
        UserView archived = users.getUser(original.username());
        assertTrue(assertThrows(BusinessConflictException.class, () -> users.updateUser(new UpdateUserCommand(
                archived.id(), archived.version(), archived.username(), "Changed", null, archived.roles())))
                .getMessage().contains("Archived"));
    }

    @Test
    void renamedDefaultAdministratorKeepsIdentityAndExistingAdministrativeAuthority() {
        UserView original = users.getUser(administrator);
        String renamed = name();
        UserView otherAdmin = users.createUser(new CreateUserCommand(name(), "Transit899!@#", "Other Admin", null,
                Set.of("SYSTEM_ADMIN")));
        try {
            users.updateUser(new UpdateUserCommand(original.id(), original.version(), renamed,
                    original.displayName(), original.email(), original.roles()));
            assertEquals(renamed, users.currentUser().username());
            // A session opened under the old name can still attribute a new action to the same user ID.
            UserView created = createStaff();
            assertEquals(original.id(), jdbc.queryForObject(
                    "SELECT assigned_by_user_id FROM arc.user_roles WHERE user_id=?", Long.class, created.id()));
            authenticateAs(renamed);
            users.deleteUser(otherAdmin.username());
            assertEquals("DISABLED", users.getUser(otherAdmin.username()).accountStatus());
        } finally {
            UserView current = users.getUser(jdbc.queryForObject(
                    "SELECT username FROM arc.app_users WHERE id=?", String.class, original.id()));
            users.updateUser(new UpdateUserCommand(current.id(), current.version(), administrator,
                    original.displayName(), original.email(), original.roles()));
        }
    }

    private UserView createStaff() {
        String username = name();
        return users.createUser(new CreateUserCommand(username, "Transit899!@#", "Test Staff", username + "@example.com",
                Set.of("OPERATIONS_STAFF")));
    }

    private String passwordHash(Long id) {
        return jdbc.queryForObject("SELECT password_hash FROM arc.app_users WHERE id=?", String.class, id);
    }

    private static String name() {
        return "edit-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }
}
