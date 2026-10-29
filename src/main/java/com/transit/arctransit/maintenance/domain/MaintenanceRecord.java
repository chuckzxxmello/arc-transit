package com.transit.arctransit.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

// maintenance record entity

@Entity
@Table(
        name = "maintenance_records",
        schema = "arc"
)
public class MaintenanceRecord {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "fleet_unit_id",
            nullable = false
    )
    private Long fleetUnitId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "maintenance_type",
            nullable = false,
            length = 30
    )
    private MaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "maintenance_status",
            nullable = false,
            length = 30
    )
    private MaintenanceStatus maintenanceStatus;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "scheduled_date"
    )
    private LocalDate scheduledDate;

    @Column(
            name = "started_at"
    )
    private Instant startedAt;

    @Column(
            name = "completed_at"
    )
    private Instant completedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(
            name = "technician_notes",
            columnDefinition = "TEXT"
    )
    private String technicianNotes;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    private Long version;

    // JPA

    protected MaintenanceRecord() {
    }

    // create maintenance request
    public MaintenanceRecord(
            Long fleetUnitId,
            MaintenanceType maintenanceType,
            String description,
            LocalDate scheduledDate) {

        this.fleetUnitId = fleetUnitId;
        this.maintenanceType = maintenanceType;
        this.maintenanceStatus =
                MaintenanceStatus.REQUESTED;
        this.description = description;
        this.scheduledDate = scheduledDate;

        Instant now =
                Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    // approve
    public void approve() {

        requireStatus(
                MaintenanceStatus.REQUESTED,
                "approve"
        );

        this.maintenanceStatus =
                MaintenanceStatus.APPROVED;

        this.updatedAt =
                Instant.now();
    }

    // start work
    public void startWork() {

        requireStatus(
                MaintenanceStatus.APPROVED,
                "start work"
        );

        Instant now =
                Instant.now();

        this.maintenanceStatus =
                MaintenanceStatus.IN_PROGRESS;

        this.startedAt = now;
        this.updatedAt = now;
    }

    // complete work
    public void complete(
            String technicianNotes) {

        requireStatus(
                MaintenanceStatus.IN_PROGRESS,
                "complete"
        );

        Instant now =
                Instant.now();

        this.maintenanceStatus =
                MaintenanceStatus.COMPLETED;

        this.technicianNotes =
                technicianNotes;

        this.completedAt = now;
        this.archivedAt = now;
        this.updatedAt = now;
    }

    // cancel maintenance
    public void cancel() {

        if (maintenanceStatus
                == MaintenanceStatus.COMPLETED
                || maintenanceStatus
                == MaintenanceStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot cancel: current status is "
                            + maintenanceStatus
            );
        }

        this.maintenanceStatus =
                MaintenanceStatus.CANCELLED;

        this.updatedAt =
                Instant.now();
    }

    // lifecycle validation
    private void requireStatus(
            MaintenanceStatus expectedStatus,
            String operation) {

        if (maintenanceStatus != expectedStatus) {

            throw new IllegalStateException(
                    "Cannot "
                            + operation
                            + ": current status is "
                            + maintenanceStatus
                            + ", expected "
                            + expectedStatus
            );
        }
    }

    // getters

    public Long getId() {
        return id;
    }

    public Long getFleetUnitId() {
        return fleetUnitId;
    }

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public MaintenanceStatus getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
