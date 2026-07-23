package com.transit.arctransit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps a role assignment to a staff account.
 */
@Entity
@Table(name = "user_roles", schema = "arc")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role_code", length = 40, nullable = false)
    private String roleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_code", insertable = false, updatable = false)
    private Role role;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRole() {
        // JPA
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Role getRole() {
        return role;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
