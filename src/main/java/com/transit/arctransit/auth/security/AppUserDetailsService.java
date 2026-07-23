package com.transit.arctransit.auth.security;

import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads staff accounts from the database for Spring Security
 * authentication.
 *
 * Each role code stored in user_roles is mapped to a Spring Security
 * granted authority with the ROLE_ prefix.
 */
@Service
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {

        AppUser appUser = userRepository
                .findByUsername(username.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for username: " + username));

        /*
         * Map each assigned role to a Spring Security authority.
         *
         * Example:
         * role_code SYSTEM_ADMIN  →  ROLE_SYSTEM_ADMIN
         */
        List<SimpleGrantedAuthority> authorities = appUser.getUserRoles()
                .stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRoleCode()))
                .toList();

        return new User(
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.isActive(),   // enabled
                true,                 // accountNonExpired
                true,                 // credentialsNonExpired
                !"LOCKED".equals(appUser.getAccountStatus()), // accountNonLocked
                authorities);
    }
}
