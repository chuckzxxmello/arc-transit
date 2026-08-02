package com.transit.arctransit.dispatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A dispatch assignment linking one bus, one driver, and one route
 * for a specific date and time.
 *
 * Maps to the arc.dispatch_assignments table created by
 * V5__create_dispatch_assignments.sql.
 *
 * Design decisions:
 *
 * 1. Foreign keys are stored as plain Long IDs (fleetUnitId, driverId,
 *    routeId) instead of @ManyToOne entity references. This respects
 *    Spring Modulith module boundaries: the dispatch module should not
 *    directly hold JPA references to entities owned by other modules.
 *    Instead, it stores the ID and calls the public service interface
 *    of each module to validate the referenced entity.
 *
 *    Source: https://docs.spring.io/spring-modulith/reference/fundamentals.html
 *    (Ctrl+F: "module boundaries")
 *
 * 2. The state machine enforces valid transitions:
 *    SCHEDULED   -> IN_PROGRESS, CANCELLED
 *    IN_PROGRESS -> COMPLETED, CANCELLED
 *    COMPLETED   -> (terminal, no further transitions)
 *    CANCELLED   -> (terminal, no further transitions)
 */
@Entity
@Table(name = "dispatch_assignments", schema = "arc")
public class DispatchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fleet_unit_id", nullable = false)
    private Long fleetUnitId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "scheduled_departure", nullable = false)
    private Instant scheduledDeparture;

    @Column(name = "scheduled_arrival")
    private Instant scheduledArrival;

    @Column(name = "actual_departure")
    private Instant actualDeparture;

    @Column(name = "actual_arrival")
    private Instant actualArrival;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", length = 20, nullable = false)
    private DispatchStatus dispatchStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    /** JPA requires a protected no-args constructor. */
    protected DispatchAssignment() {
    }

    /**
     * Creates a new dispatch assignment in SCHEDULED state.
     */
    public DispatchAssignment(Long fleetUnitId, Long driverId, Long routeId,
                              LocalDate dispatchDate, Instant scheduledDeparture,
                              Instant scheduledArrival, String notes) {
        this.fleetUnitId = fleetUnitId;
        this.driverId = driverId;
        this.routeId = routeId;
        this.dispatchDate = dispatchDate;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.notes = notes;
        this.dispatchStatus = DispatchStatus.SCHEDULED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // --- State Machine Methods ---

    /**
     * Transitions SCHEDULED -> IN_PROGRESS.
     * Records the actual departure time.
     */
    public void startTrip() {
        if (dispatchStatus != DispatchStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot start trip: current status is " + dispatchStatus + ", expected SCHEDULED");
        }
        this.dispatchStatus = DispatchStatus.IN_PROGRESS;
        this.actualDeparture = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions IN_PROGRESS -> COMPLETED.
     * Records the actual arrival time.
     */
    public void completeTrip() {
        if (dispatchStatus != DispatchStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot complete trip: current status is " + dispatchStatus + ", expected IN_PROGRESS");
        }
        this.dispatchStatus = DispatchStatus.COMPLETED;
        this.actualArrival = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions SCHEDULED or IN_PROGRESS -> CANCELLED.
     */
    public void cancel() {
        if (dispatchStatus == DispatchStatus.COMPLETED || dispatchStatus == DispatchStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot cancel: current status is " + dispatchStatus + " (already terminal)");
        }
        this.dispatchStatus = DispatchStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Long getFleetUnitId() { return fleetUnitId; }
    public Long getDriverId() { return driverId; }
    public Long getRouteId() { return routeId; }
    public LocalDate getDispatchDate() { return dispatchDate; }
    public Instant getScheduledDeparture() { return scheduledDeparture; }
    public Instant getScheduledArrival() { return scheduledArrival; }
    public Instant getActualDeparture() { return actualDeparture; }
    public Instant getActualArrival() { return actualArrival; }
    public DispatchStatus getDispatchStatus() { return dispatchStatus; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
