package com.transit.arctransit.auth.security;

import com.transit.arctransit.auth.domain.AccountStatus;
import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// load staff accounts from the database for Spring Security

@Service
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {

        String normalizedUsername = normalizeUsername(username);

        AppUser appUser = userRepository
					                .findByUsername(normalizedUsername)
					                .orElseThrow(() ->
					                        new UsernameNotFoundException("Account not found.")
        );

        List<SimpleGrantedAuthority> authorities = appUser.getUserRoles()
								                          .stream()
								                          .map(userRole -> new SimpleGrantedAuthority(
								                                					ROLE_PREFIX + userRole.getRoleCode())
								                          )
								                          .toList();

        return new User(
	                appUser.getUsername(),
	                appUser.getPasswordHash(),
	                appUser.isActive(),
	                true,
	                true,
	                appUser.getAccountStatus() != AccountStatus.LOCKED,
	                												authorities
        );
    }

    private static String normalizeUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Account not found.");
        }

        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}