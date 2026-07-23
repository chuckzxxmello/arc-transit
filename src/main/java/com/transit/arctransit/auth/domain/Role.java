package com.transit.arctransit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Immutable role definition seeded by Flyway.
 *
 * Version 1 roles:
 * - SYSTEM_ADMIN
 * - OPERATIONS_STAFF
 */
@Entity
@Table(name = "roles", schema = "arc")
public class Role {

    @Id
    @Column(name = "role_code", length = 40, nullable = false)
    private String roleCode;

    @Column(name = "role_name", length = 80, nullable = false)
    private String roleName;

    @Column(name = "description")
    private String description;

    protected Role() {
        // JPA
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }
}
