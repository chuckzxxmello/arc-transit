package com.transit.arctransit.auth.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// data access for staff accounts.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    Page<AppUser> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
		    String username,
		    String displayName,
		    Pageable pageable);
}