package com.transit.arctransit.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for application roles.
 */
public interface RoleRepository extends JpaRepository<Role, String> {
}
