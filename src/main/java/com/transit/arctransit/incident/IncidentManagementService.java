package com.transit.arctransit.incident;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// public application contract for Incident Management
public interface IncidentManagementService {

    Page<IncidentView> searchArchivedIncidents(@Size(max = 200) @SafeText String filter, Pageable pageable);

    // report a new incident
    IncidentView reportIncident(
            @NotNull @Valid CreateIncidentCommand command
    );

    // move REPORTED -> ACKNOWLEDGED
    IncidentView acknowledgeIncident(
            @NotNull @Positive Long incidentId
    );

    // move ACKNOWLEDGED -> INVESTIGATING
    IncidentView startInvestigation(
            @NotNull @Positive Long incidentId
    );

    // move INVESTIGATING -> RESOLVED
    IncidentView resolveIncident(
            @NotNull @Positive Long incidentId,
            @NotBlank @Size(max = 500) @SafeText(multiline = true) String resolutionNotes
    );

    // move RESOLVED -> CLOSED
    IncidentView closeIncident(
            @NotNull @Positive Long incidentId
    );

    IncidentView linkMaintenanceRecord(@NotNull @Positive Long incidentId, @NotNull @Positive Long maintenanceRecordId);

    IncidentView unlinkMaintenanceRecord(@NotNull @Positive Long incidentId,
            @NotNull @Positive Long expectedMaintenanceRecordId,
            @NotBlank @Size(max = 500) @SafeText(multiline = true) String reason);

    List<IncidentEventView> getHistory(@NotNull @Positive Long incidentId);

    // return latest incidents
    List<IncidentView> getRecentIncidents();

    // count incidents that still require attention
    long countOpenIncidents();

    // return non-archived incidents
    Page<IncidentView> searchIncidents(
            Pageable pageable
    );
}
