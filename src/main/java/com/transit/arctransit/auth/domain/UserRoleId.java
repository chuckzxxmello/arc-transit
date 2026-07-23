package com.transit.arctransit.auth.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the user_roles table.
 */
public class UserRoleId implements Serializable {

    private Long userId;
    private String roleCode;

    public UserRoleId() {
    }

    public UserRoleId(Long userId, String roleCode) {
        this.userId = userId;
        this.roleCode = roleCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(roleCode, that.roleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleCode);
    }
}
