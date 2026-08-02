package com.transit.arctransit.fleet.domain;

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

/**
 * Master identity and operational state of a bus registered in the Arc Transit fleet.
 *
 * Maps to the arc.fleet_units table created by V1__create_fleet_units.sql.
 *
 * Design decisions:
 *
 * 1. @Enumerated(EnumType.STRING) is used for both vehicleType and operationalStatus
 *    so that Hibernate stores the Java enum constant name directly as the VARCHAR
 *    column value. This makes the database human-readable and avoids the fragility
 *    of ordinal-based enum mapping.
 *    Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a14935
 *    (Ctrl+F: EnumType.STRING)
 *
 * 2. @Version enables JPA optimistic locking. If two transactions attempt to update
 *    the same fleet unit concurrently, the second one receives an
 *    OptimisticLockException instead of silently overwriting the first.
 *    Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a5309
 *    (Ctrl+F: @Version)
 *
 * 3. Soft-delete via archivedAt: fleet units are never physically deleted from
 *    the database. Instead, the archivedAt timestamp is set to mark the record
 *    as archived. This preserves historical links to dispatch, maintenance,
 *    incident, and audit records.
 */
@Entity
@Table(name = "fleet_units", schema = "arc")
public class FleetUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_number", length = 30, nullable = false, unique = true)
    private String unitNumber;

    @Column(name = "plate_number", length = 20, nullable = false, unique = true)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 30, nullable = false)
    private VehicleType vehicleType;

    @Column(name = "capacity", nullable = false)
    private Short capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", length = 30, nullable = false)
    private OperationalStatus operationalStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    /** JPA requires a protected no-args constructor. */
    protected FleetUnit() {
    }

    /**
     * Creates a new fleet unit with INACTIVE status.
     * A newly registered bus does not automatically become available for dispatch.
     */
    public FleetUnit(String unitNumber, String plateNumber, VehicleType vehicleType, short capacity) {
        this.unitNumber = unitNumber;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.operationalStatus = OperationalStatus.INACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // --- Domain Methods ---

    /** Marks this bus as available for dispatch. */
    public void activate() {
        this.operationalStatus = OperationalStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Marks this bus as unavailable for dispatch. */
    public void deactivate() {
        this.operationalStatus = OperationalStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Marks this bus as undergoing maintenance. */
    public void markUnderMaintenance() {
        this.operationalStatus = OperationalStatus.UNDER_MAINTENANCE;
        this.updatedAt = Instant.now();
    }

    /** Marks this bus as permanently removed from service. */
    public void markOutOfService() {
        this.operationalStatus = OperationalStatus.OUT_OF_SERVICE;
        this.updatedAt = Instant.now();
    }

    /** Changes the operational status to the specified value. */
    public void changeOperationalStatus(OperationalStatus newStatus) {
        this.operationalStatus = newStatus;
        this.updatedAt = Instant.now();
    }

    /** Soft-deletes this fleet unit by setting the archive timestamp. */
    public void archive() {
        this.archivedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Restores this fleet unit from the archive. */
    public void unarchive() {
        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    /** Returns true when this fleet unit has not been archived. */
    public boolean isActive() {
        return archivedAt == null;
    }

    // --- Field Updates ---

    public void updateDetails(String unitNumber, String plateNumber, short capacity) {
        this.unitNumber = unitNumber;
        this.plateNumber = plateNumber;
        this.capacity = capacity;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getUnitNumber() { return unitNumber; }
    public String getPlateNumber() { return plateNumber; }
    public VehicleType getVehicleType() { return vehicleType; }
    public Short getCapacity() { return capacity; }
    public OperationalStatus getOperationalStatus() { return operationalStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
}
