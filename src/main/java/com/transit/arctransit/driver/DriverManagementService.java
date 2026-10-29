package com.transit.arctransit.driver;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for Driver Management
public interface DriverManagementService {

    DriverView lockDriverForAssignment(@NotNull @Positive Long id);

    DriverView changeAvailability(@NotNull @Valid ChangeDriverAvailabilityCommand command);

    Page<DriverAvailabilityHistoryView> availabilityHistory(@NotNull @Positive Long id, Pageable pageable);

    DriverView registerDriver(@NotNull @Valid CreateDriverCommand command);
    DriverView updateDriver(@NotNull @Valid UpdateDriverCommand command);
    DriverView changeEmploymentStatus(@NotNull @Positive Long id, @NotBlank @Size(max = 50) @SafeText String newStatus);

    // archive driver by setting their archive timestamp
    void archiveDriver(@NotNull @Positive Long id);

    // restores driver from the archive
    void unarchiveDriver(@NotNull @Positive Long id);

    // permanently delete driver from the database
    void hardDeleteDriver(@NotNull @Positive Long id);

    // search active drivers for management list views
    Page<DriverSummaryView> searchDrivers(@Valid DriverQuery query, Pageable pageable);

    Page<DriverSummaryView> searchDispatchCandidates(
            @Size(max = 200) @SafeText String filter, Pageable pageable);

    // search archived drivers for the archive view
    Page<DriverSummaryView> searchArchivedDrivers(Pageable pageable);

    // retrieve single driver by ID
    DriverView getDriver(@NotNull @Positive Long id);
}
