package com.transit.arctransit;

import com.transit.arctransit.auth.ChangeAccountStatusCommand;
import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.UserQuery;
import com.transit.arctransit.auth.UserSummaryView;
import com.transit.arctransit.common.exception.BusinessConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class UserArchiveIntegrationTest {
    @Autowired private UserAdministrationService users;
    @Autowired private UserDetailsService authenticationUsers;
    @Autowired private JdbcTemplate jdbc;
    @Value("${arc.security.admin.username}") private String administratorUsername;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                administratorUsername, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void archiveCommitsAndMovesAccountBetweenListsWithoutDeletingIdentityOrRoles() {
        String username = "qa-archive-" + suffix();
        create(username, "Archive Integration Staff");
        var before = jdbc.queryForMap("SELECT id, password_hash FROM arc.app_users WHERE username = ?", username);
        assertTrue(authenticationUsers.loadUserByUsername(username).isEnabled());
        assertEquals(1, users.searchUsers(new UserQuery(username), PageRequest.of(0, 10)).getTotalElements());

        users.deleteUser(username);

        var archived = jdbc.queryForMap("SELECT id, password_hash, archived_at, account_status "
                + "FROM arc.app_users WHERE username = ?", username);
        assertEquals(before.get("id"), archived.get("id"));
        assertEquals(before.get("password_hash"), archived.get("password_hash"));
        assertNotNull(archived.get("archived_at"));
        assertEquals("DISABLED", archived.get("account_status"));
        assertFalse(authenticationUsers.loadUserByUsername(username).isEnabled());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM arc.user_roles WHERE user_id = ? "
                + "AND role_code = 'OPERATIONS_STAFF'", Integer.class, before.get("id")));
        assertEquals(0, users.searchUsers(new UserQuery(username), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(List.of(username), users.searchArchivedUsers(new UserQuery(username), PageRequest.of(0, 10))
                .map(UserSummaryView::username).getContent());
        assertThrows(BusinessConflictException.class, () -> create(username, "Duplicate identity"));
    }

    @Test
    void searchGroupsUsernameAndNameMatchesWithinTheCorrectArchiveState() {
        String prefix = "qa-search-" + suffix();
        create(prefix + "-a", prefix + " Alpha");
        create(prefix + "-b", prefix + " Bravo");
        create(prefix + "-c", prefix + " Charlie");
        users.deleteUser(prefix + "-a");
        users.deleteUser(prefix + "-b");
        users.changeAccountStatus(new ChangeAccountStatusCommand(prefix + "-c", "DISABLED"));

        var pageable = PageRequest.of(0, 1, Sort.by("username"));
        var first = users.searchArchivedUsers(new UserQuery(" " + prefix.toUpperCase() + " "), pageable);
        var second = users.searchArchivedUsers(new UserQuery(prefix), pageable.next());
        assertEquals(2, first.getTotalElements());
        assertEquals(prefix + "-a", first.getContent().getFirst().username());
        assertEquals(prefix + "-b", second.getContent().getFirst().username());
        assertEquals(1, users.searchArchivedUsers(new UserQuery(prefix.toUpperCase() + " ALPHA"), pageable)
                .getTotalElements());
        assertEquals(0, users.searchUsers(new UserQuery(prefix.toUpperCase() + " ALPHA"), pageable)
                .getTotalElements());
        var unarchived = users.searchUsers(new UserQuery(prefix), pageable);
        assertEquals(1, unarchived.getTotalElements());
        assertEquals(prefix + "-c", unarchived.getContent().getFirst().username());
        assertEquals("DISABLED", unarchived.getContent().getFirst().accountStatus());
    }

    @Test
    void missingAndBlankFiltersReturnUsableSeparatedLists() {
        String username = "qa-blank-" + suffix();
        create(username, "Blank Search Staff");
        users.deleteUser(username);
        for (UserQuery query : new UserQuery[]{null, new UserQuery(null), new UserQuery(""), new UserQuery("  ")}) {
            var pageable = PageRequest.of(0, 100, Sort.by("username"));
            assertTrue(users.searchArchivedUsers(query, pageable).stream()
                    .anyMatch(user -> username.equals(user.username())));
            assertFalse(users.searchUsers(query, pageable).stream()
                    .anyMatch(user -> username.equals(user.username())));
        }
    }

    private void create(String username, String name) {
        users.createUser(new CreateUserCommand(username, "Cedar9#Sea4!Moon", name, null,
                Set.of("OPERATIONS_STAFF")));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
