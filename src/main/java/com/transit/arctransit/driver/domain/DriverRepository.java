package com.transit.arctransit.driver.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// data access for driver records
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmployeeNumber(String employeeNumber);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    // return non-archived drivers for main list view
    Page<Driver> findByArchivedAtIsNull(Pageable pageable);

    // return archived drivers for the archive view
    Page<Driver> findByArchivedAtIsNotNull(Pageable pageable);
}
