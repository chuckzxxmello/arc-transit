package com.transit.arctransit.incident;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// public application contract for Incident Management
public interface IncidentManagementService {

    // report a new incident
    IncidentView reportIncident(
            @Valid CreateIncidentCommand command
    );

    // move REPORTED -> ACKNOWLEDGED
    IncidentView acknowledgeIncident(
            Long incidentId
    );

    // move ACKNOWLEDGED -> INVESTIGATING
    IncidentView startInvestigation(
            Long incidentId
    );

    // move INVESTIGATING -> RESOLVED
    IncidentView resolveIncident(
            Long incidentId,
            String resolutionNotes
    );

    // move RESOLVED -> CLOSED
    IncidentView closeIncident(
            Long incidentId
    );

    // return latest incidents
    List<IncidentView> getRecentIncidents();

    // count incidents that still require attention
    long countOpenIncidents();

    // return non-archived incidents
    Page<IncidentView> searchIncidents(
            Pageable pageable
    );
}