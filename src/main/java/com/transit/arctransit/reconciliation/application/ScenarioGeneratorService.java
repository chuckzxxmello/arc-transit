package com.transit.arctransit.reconciliation.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.incident.CreateIncidentCommand;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.maintenance.MaintenanceService;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

// development-only QA scenario generator

@Service
@Profile("dev")
@Transactional
public class ScenarioGeneratorService {

    private final FleetManagementService fleetService;
    private final MaintenanceService maintenanceService;
    private final IncidentManagementService incidentService;
    private final AuditRecordingService auditService;

    public ScenarioGeneratorService(
            FleetManagementService fleetService,
            MaintenanceService maintenanceService,
            IncidentManagementService incidentService,
            AuditRecordingService auditService) {

        this.fleetService = fleetService;
        this.maintenanceService = maintenanceService;
        this.incidentService = incidentService;
        this.auditService = auditService;
    }

    // generate active fleet + active maintenance contradiction
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void generateContradictionScenario() {

        FleetUnitSummaryView unit =
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getContent()
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No fleet units available."
                                )
                        );

        MaintenanceRecordView record =
                maintenanceService
                        .requestMaintenance(
                                new CreateMaintenanceCommand(
                                        unit.id(),
                                        "EMERGENCY",
                                        "Simulated breakdown",
                                        LocalDate.now()
                                )
                        );

        maintenanceService.approveMaintenance(
                record.id()
        );

        maintenanceService.startMaintenance(
                record.id()
        );

        // deliberately restore ACTIVE to create contradiction

        fleetService.changeStatus(
                unit.id(),
                "ACTIVE"
        );

        auditService.recordAction(
                "SCENARIO_GENERATED",
                "ScenarioGenerator",
                0L,
                "Created active fleet unit with active maintenance."
        );
    }

    // generate unresolved incident

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void generateIncidentScenario() {

        FleetUnitSummaryView unit =
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getContent()
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No fleet units available."
                                )
                        );

        incidentService.reportIncident(
                new CreateIncidentCommand(
                        "Simulated Passenger Disturbance",
                        "Passenger disturbance on unit "
                                + unit.unitNumber(),
                        "MEDIUM",
                        "FLEET",
                        unit.id()
                )
        );

        auditService.recordAction(
                "SCENARIO_GENERATED",
                "ScenarioGenerator",
                0L,
                "Created simulated unresolved incident."
        );
    }
}