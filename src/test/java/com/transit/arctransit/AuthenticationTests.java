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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;

// unit tests for authentication user mapping

@ExtendWith(MockitoExtension.class)
class AuthenticationTests {

    // mocked database repository

    @Mock
    private AppUserRepository userRepository;

    // class under test

    @InjectMocks
    private AppUserDetailsService userDetailsService;

    // active account

    @Test
    void loadUserByUsername_activeUser_returnsValidUserDetails() {

        AppUser activeUser =
                new AppUser(
                        "test.admin",
                        "hash",
                        "Test Admin",
                        "admin@arc.local"
                );

        when(
                userRepository.findByUsername("test.admin")
        )
        .thenReturn(
                Optional.of(activeUser)
        );

        UserDetails details =
                userDetailsService
                        .loadUserByUsername(
                                "test.admin"
                        );

        assertTrue(
                details.isEnabled(),
                "Active user should be enabled"
        );

        assertTrue(
                details.isAccountNonLocked(),
                "Active user should not be locked"
        );

        assertTrue(
                details.isAccountNonExpired(),
                "Active user should not be expired"
        );

        assertTrue(
                details.isCredentialsNonExpired(),
                "Active user credentials should not be expired"
        );

        assertEquals(
                "test.admin",
                details.getUsername()
        );
    }

    // locked account

    @Test
    void loadUserByUsername_lockedUser_returnsLockedUserDetails() {

        AppUser lockedUser =
                new AppUser(
                        "locked.staff",
                        "hash",
                        "Locked Staff",
                        "locked@arc.local"
                );

        lockedUser.changeAccountStatus(
                AccountStatus.LOCKED
        );

        when(
                userRepository.findByUsername(
                        "locked.staff"
                )
        )
        .thenReturn(
                Optional.of(lockedUser)
        );

        UserDetails details =
                userDetailsService
                        .loadUserByUsername(
                                "locked.staff"
                        );

        assertFalse(
                details.isEnabled(),
                "Locked user should be disabled"
        );

        assertFalse(
                details.isAccountNonLocked(),
                "Locked user should not be account-non-locked"
        );
    }

    // disabled account

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledUserDetails() {

        AppUser disabledUser =
                new AppUser(
                        "disabled.staff",
                        "hash",
                        "Disabled Staff",
                        "disabled@arc.local"
                );

        disabledUser.changeAccountStatus(
                AccountStatus.DISABLED
        );

        when(
                userRepository.findByUsername(
                        "disabled.staff"
                )
        )
        .thenReturn(
                Optional.of(disabledUser)
        );

        UserDetails details =
                userDetailsService
                        .loadUserByUsername(
                                "disabled.staff"
                        );

        assertFalse(
                details.isEnabled(),
                "Disabled user should not be enabled"
        );

        assertTrue(
                details.isAccountNonLocked(),
                "Disabled user should not be considered locked"
        );
    }

    // nonexistent account

    @Test
    void loadUserByUsername_notFound_throwsException() {

        when(
                userRepository.findByUsername(
                        "nobody"
                )
        )
        .thenReturn(
                Optional.empty()
        );

        assertThrows(
                UsernameNotFoundException.class,
                () ->
                        userDetailsService
                                .loadUserByUsername(
                                        "nobody"
                                )
        );
    }
}