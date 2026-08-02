package com.transit.arctransit.analytics.application;

import com.transit.arctransit.analytics.domain.Incident;
import com.transit.arctransit.analytics.domain.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    public List<Incident> getRecentIncidents() {
        return repository.findTop5ByOrderByReportedAtDesc();
    }
}
