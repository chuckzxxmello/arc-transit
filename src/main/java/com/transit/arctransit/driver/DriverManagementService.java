package com.transit.arctransit.driver;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service contract for Driver Management.
 */
public interface DriverManagementService {

    DriverView registerDriver(@Valid CreateDriverCommand command);

    DriverView updateDriver(@Valid UpdateDriverCommand command);

    DriverView changeEmploymentStatus(Long id, String newStatus);

    /** Soft-deletes a driver by setting their archive timestamp. */
    void archiveDriver(Long id);

    /** Restores a driver from the archive. */
    void unarchiveDriver(Long id);

    /** Permanently deletes a driver from the database. */
    void hardDeleteDriver(Long id);

    /** Searches active drivers for management list views. */
    Page<DriverSummaryView> searchDrivers(DriverQuery query, Pageable pageable);

    /** Searches soft-deleted drivers for the archive view. */
    Page<DriverSummaryView> searchArchivedDrivers(Pageable pageable);

    /** Retrieves a single driver by ID. */
    DriverView getDriver(Long id);
}
