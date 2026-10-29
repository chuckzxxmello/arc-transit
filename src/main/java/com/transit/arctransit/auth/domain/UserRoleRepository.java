package com.transit.arctransit.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	void deleteByUserId(Long userId);

    boolean existsByAssignedByUserIdAndUserIdNot(Long assignedByUserId, Long userId);
	
	List<UserRole> findByUserId(Long userId);
	
	boolean existsByUserIdAndRoleCode(
							    Long userId,
							    String roleCode
	);
	
	long countByRoleCode(String roleCode);
}
