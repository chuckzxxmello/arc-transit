package com.transit.arctransit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Authenticated staff account stored in arc.app_users.
 *
 * Passwords are stored as BCrypt hashes and verified
 * by Spring Security through AppUserDetailsService.
 */
@Entity
@Table(name = "app_users", schema = "arc")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "account_status", length = 20, nullable = false)
    private String accountStatus;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Set<UserRole> userRoles = new HashSet<>();

    protected AppUser() {
        // JPA
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    /**
     * Returns true when the account is allowed to authenticate.
     */
    public boolean isActive() {
        return "ACTIVE".equals(accountStatus) && archivedAt == null;
    }
}
