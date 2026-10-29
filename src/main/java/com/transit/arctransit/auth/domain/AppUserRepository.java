package com.transit.arctransit.auth.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// data access for staff accounts.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.username = :username")
    Optional<AppUser> findLockedByUsername(@Param("username") String username);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM arc.username_reservations WHERE username = :username)",
            nativeQuery = true)
    boolean isUsernameReserved(@Param("username") String username);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM arc.username_reservations
                           WHERE username = :username AND user_id <> :userId)
            """, nativeQuery = true)
    boolean isUsernameReservedByOtherUser(@Param("username") String username, @Param("userId") Long userId);

    @Query(value = """
            SELECT account.* FROM arc.app_users account
            JOIN arc.username_reservations reservation ON reservation.user_id = account.id
            WHERE reservation.username = :username
            """, nativeQuery = true)
    Optional<AppUser> findByReservedUsername(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM AppUser u
            WHERE u.accountStatus = com.transit.arctransit.auth.domain.AccountStatus.ACTIVE
            AND u.archivedAt IS NULL
            AND EXISTS (SELECT r FROM UserRole r WHERE r.userId = u.id
                        AND r.roleCode = 'SYSTEM_ADMIN')
            ORDER BY u.id
            """)
    List<AppUser> findActiveAdministratorsForUpdate();
    
    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    
    @Query("""
            SELECT u FROM AppUser u
            WHERE u.archivedAt IS NULL
              AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AppUser> searchUnarchivedUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.archivedAt IS NOT NULL
              AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AppUser> searchArchivedUsers(@Param("keyword") String keyword, Pageable pageable);
}
