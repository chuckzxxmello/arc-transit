package com.transit.arctransit;

import com.transit.arctransit.auth.domain.AccountStatus;
import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;
import com.transit.arctransit.auth.security.AppUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationTests {

    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_activeUser_returnsValidUserDetails() {
        // Human-understandable comment: We mock the database response to test the security logic in isolation.
        AppUser activeUser = new AppUser("test.admin", "hash", "Test Admin", "admin@arc.local");
        
        when(userRepository.findByUsername("test.admin")).thenReturn(Optional.of(activeUser));

        UserDetails details = userDetailsService.loadUserByUsername("test.admin");

        assertTrue(details.isEnabled(), "Active user should be enabled");
        assertTrue(details.isAccountNonLocked(), "Active user should not be locked");
        assertEquals("test.admin", details.getUsername());
    }

    @Test
    void loadUserByUsername_lockedUser_returnsLockedUserDetails() {
        // Human-understandable comment: This proves that our Enum == logic successfully marks a locked user as locked in Spring Security.
        AppUser lockedUser = new AppUser("locked.staff", "hash", "Locked Staff", "locked@arc.local");
        lockedUser.changeAccountStatus(AccountStatus.LOCKED);

        when(userRepository.findByUsername("locked.staff")).thenReturn(Optional.of(lockedUser));

        UserDetails details = userDetailsService.loadUserByUsername("locked.staff");

        assertFalse(details.isAccountNonLocked(), "Locked user should have accountNonLocked = false");
        assertFalse(details.isEnabled(), "Locked user (not ACTIVE) should have enabled = false");
    }

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledUserDetails() {
        AppUser disabledUser = new AppUser("disabled.staff", "hash", "Disabled Staff", "disabled@arc.local");
        disabledUser.changeAccountStatus(AccountStatus.DISABLED);

        when(userRepository.findByUsername("disabled.staff")).thenReturn(Optional.of(disabledUser));

        UserDetails details = userDetailsService.loadUserByUsername("disabled.staff");

        assertTrue(details.isAccountNonLocked(), "Disabled user is not strictly locked, just disabled");
        assertFalse(details.isEnabled(), "Disabled user should have enabled = false");
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nobody");
        });
    }
}
