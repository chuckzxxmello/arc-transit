package com.transit.arctransit.incident.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// data access for incidents
public interface IncidentRepository
        extends JpaRepository<Incident, Long> {

    // return latest ten incidents
    List<Incident> findTop10ByOrderByReportedAtDesc();

    // retained repository query for existing callers
    Page<Incident> findByStatusNot(
            IncidentStatus status,
            Pageable pageable
    );

    // count incidents by lifecycle states
    long countByStatusIn(
            List<IncidentStatus> statuses
    );

    // return incidents matching any lifecycle state
    List<Incident> findByStatusIn(
            List<IncidentStatus> statuses
    );

    // return active non-archived incidents
    Page<Incident> findByArchivedAtIsNull(
            Pageable pageable
    );
}