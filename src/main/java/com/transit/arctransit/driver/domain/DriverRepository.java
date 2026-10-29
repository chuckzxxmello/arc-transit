package com.transit.arctransit.driver.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

// data access for driver records
public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT record FROM Driver record WHERE record.id = :id")
    Optional<Driver> findLockedById(@Param("id") Long id);

    Optional<Driver> findByEmployeeNumber(String employeeNumber);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    @Query("""
            SELECT driver FROM Driver driver
            WHERE driver.archivedAt IS NULL
              AND (:search IS NULL OR :search = '' OR LOWER(driver.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(driver.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(driver.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(driver.firstName, ' ', driver.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(driver.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR driver.employmentStatus = :status)
            """)
    Page<Driver> searchActive(@Param("search") String search,
            @Param("status") EmploymentStatus status, Pageable pageable);

    @Query("""
            SELECT driver FROM Driver driver
            WHERE driver.archivedAt IS NULL
              AND driver.employmentStatus = com.transit.arctransit.driver.domain.EmploymentStatus.ACTIVE
              AND driver.licenseExpiryDate >= :today
              AND (:search IS NULL OR :search = ''
                   OR LOWER(driver.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(driver.firstName, ' ', driver.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Driver> searchDispatchCandidates(@Param("search") String search,
            @Param("today") java.time.LocalDate today, Pageable pageable);

    // return non-archived drivers for main list view
    Page<Driver> findByArchivedAtIsNull(Pageable pageable);

    // return archived drivers for the archive view
    Page<Driver> findByArchivedAtIsNotNull(Pageable pageable);
}
