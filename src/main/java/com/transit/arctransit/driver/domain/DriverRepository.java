package com.transit.arctransit.driver.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for driver records.
 *
 * Source: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
 * (Ctrl+F: JpaRepository)
 */
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmployeeNumber(String employeeNumber);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    /** Returns only non-archived drivers for management list views. */
    Page<Driver> findByArchivedAtIsNull(Pageable pageable);

    /** Returns only archived drivers for the archive view. */
    Page<Driver> findByArchivedAtIsNotNull(Pageable pageable);
}
