package com.transit.arctransit.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    void deleteByUserId(Long userId);
    List<UserRole> findByUserId(Long userId);
}
