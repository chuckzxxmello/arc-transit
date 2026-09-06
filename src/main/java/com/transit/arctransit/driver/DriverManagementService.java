package com.transit.arctransit.driver;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for Driver Management
public interface DriverManagementService {

    DriverView registerDriver(@Valid CreateDriverCommand command);
    DriverView updateDriver(@Valid UpdateDriverCommand command);
    DriverView changeEmploymentStatus(Long id, String newStatus);

    // archive driver by setting their archive timestamp
    void archiveDriver(Long id);

    // restores driver from the archive
    void unarchiveDriver(Long id);

    // permanently delete driver from the database
    void hardDeleteDriver(Long id);

    // search active drivers for management list views
    Page<DriverSummaryView> searchDrivers(DriverQuery query, Pageable pageable);

    // search archived drivers for the archive view
    Page<DriverSummaryView> searchArchivedDrivers(Pageable pageable);

    // retrieve single driver by ID
    DriverView getDriver(Long id);
}
