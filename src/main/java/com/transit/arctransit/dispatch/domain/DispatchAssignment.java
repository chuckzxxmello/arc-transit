package com.transit.arctransit.dispatch.domain;

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



// dispatch assignment domain entity
@Entity
@Table(
        name = "dispatch_assignments",
        schema = "arc"
)
public class DispatchAssignment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    // cross-module references are stored as IDs

    @Column(
            name = "fleet_unit_id",
            nullable = false
    )
    private Long fleetUnitId;

    @Column(
            name = "driver_id",
            nullable = false
    )
    private Long driverId;

    @Column(
            name = "route_id",
            nullable = false
    )
    private Long routeId;

    @Column(name = "route_schedule_id")
    private Long scheduleId;

    @Column(name = "route_version_id", nullable = false)
    private Long routeVersionId;

    @Column(
            name = "dispatch_date",
            nullable = false
    )
    private LocalDate dispatchDate;

    @Column(
            name = "scheduled_departure",
            nullable = false
    )
    private Instant scheduledDeparture;

    @Column(
            name = "scheduled_arrival"
    )
    private Instant scheduledArrival;

    @Column(
            name = "actual_departure"
    )
    private Instant actualDeparture;

    @Column(
            name = "actual_arrival"
    )
    private Instant actualArrival;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "dispatch_status",
            length = 20,
            nullable = false
    )
    private DispatchStatus dispatchStatus;

    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

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

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    // JPA

    protected DispatchAssignment() {
    }

    // create assignment

    public DispatchAssignment(
            Long fleetUnitId,
            Long driverId,
            Long routeId,
            LocalDate dispatchDate,
            Instant scheduledDeparture,
            Instant scheduledArrival,
            String notes) {

        this.fleetUnitId = fleetUnitId;
        this.driverId = driverId;
        this.routeId = routeId;
        this.dispatchDate = dispatchDate;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.notes = notes;
        this.dispatchStatus =
                DispatchStatus.SCHEDULED;

        Instant now =
                Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    public void linkRouteVersion(Long routeVersionId) {
        if (this.routeVersionId != null || routeVersionId == null) {
            throw new IllegalArgumentException("An assignment must retain its original route version.");
        }
        this.routeVersionId = routeVersionId;
    }

    public void linkSchedule(Long scheduleId) {
        if (this.scheduleId != null) {
            throw new IllegalArgumentException("An assignment must retain its original schedule.");
        }
        this.scheduleId = scheduleId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getRouteVersionId() {
        return routeVersionId;
    }

    // start trip

    public void startTrip() {

        requireStatus(
                DispatchStatus.SCHEDULED,
                "start trip"
        );

        Instant now =
                Instant.now();

        this.dispatchStatus =
                DispatchStatus.IN_PROGRESS;

        this.actualDeparture = now;
        this.updatedAt = now;
    }

    // complete trip

    public void completeTrip() {

        requireStatus(
                DispatchStatus.IN_PROGRESS,
                "complete trip"
        );

        Instant now =
                Instant.now();

        this.dispatchStatus =
                DispatchStatus.COMPLETED;

        this.actualArrival = now;
        this.updatedAt = now;
    }

    // cancel trip

    public void cancel() {

        if (dispatchStatus == DispatchStatus.COMPLETED
                || dispatchStatus == DispatchStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot cancel: current status is "
                            + dispatchStatus
                            + " (already terminal)."
            );
        }

        this.dispatchStatus =
                DispatchStatus.CANCELLED;

        this.updatedAt =
                Instant.now();
    }

    // archive

    public void archive() {

        if (archivedAt != null) {
            return;
        }

        Instant now =
                Instant.now();

        this.archivedAt = now;
        this.updatedAt = now;
    }

    // restore

    public void unarchive() {

        if (archivedAt == null) {
            return;
        }

        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    // status validation

    private void requireStatus(
            DispatchStatus expectedStatus,
            String operation) {

        if (dispatchStatus != expectedStatus) {

            throw new IllegalStateException(
                    "Cannot "
                            + operation
                            + ": current status is "
                            + dispatchStatus
                            + ", expected "
                            + expectedStatus
            );
        }
    }

    // archive state

    public boolean isArchived() {
        return archivedAt != null;
    }

    // getters

    public Long getId() {
        return id;
    }

    public Long getFleetUnitId() {
        return fleetUnitId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public Instant getActualDeparture() {
        return actualDeparture;
    }

    public Instant getActualArrival() {
        return actualArrival;
    }

    public DispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
