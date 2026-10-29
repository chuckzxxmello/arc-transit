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

// identity and operational state of bus fleet units
@Entity
@Table(name = "fleet_units", schema = "arc")
public class FleetUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "unit_number",
            length = 30,
            nullable = false,
            unique = true
    )
    private String unitNumber;

    @Column(
            name = "plate_number",
            length = 20,
            nullable = false,
            unique = true
    )
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "vehicle_type",
            length = 30,
            nullable = false
    )
    private VehicleType vehicleType;

    @Column(name = "capacity", nullable = false)
    private Short capacity;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "operational_status",
            length = 30,
            nullable = false
    )
    private OperationalStatus operationalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "administrative_status", nullable = false, length = 30)
    private AdministrativeStatus administrativeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false, length = 30)
    private VehicleAvailability availability;

    @Column(name = "availability_effective_at", nullable = false)
    private Instant availabilityEffectiveAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    // Required by JPA
    protected FleetUnit() {
    }

    // create new bus fleet unit
    public FleetUnit(
		            String unitNumber,
		            String plateNumber,
		            VehicleType vehicleType,
		            short capacity) {

    requireCapacity(capacity);
    this.unitNumber = unitNumber;
    this.plateNumber = plateNumber;
    this.vehicleType = vehicleType;
    this.capacity = capacity;
    this.operationalStatus = OperationalStatus.INACTIVE;
    this.administrativeStatus = AdministrativeStatus.INACTIVE;
    this.availability = VehicleAvailability.UNKNOWN;
    this.availabilityEffectiveAt = Instant.now();

    Instant now = Instant.now();
    
    this.createdAt = now;
    this.updatedAt = now;
    }

    // operational status fleet changes
    public void activate() {
		changeOperationalStatus(OperationalStatus.ACTIVE);
    }

    public void deactivate() {
		changeOperationalStatus(OperationalStatus.INACTIVE);
    }

    public void markUnderMaintenance() {
		changeOperationalStatus(OperationalStatus.UNDER_MAINTENANCE);
    }

    public void markOutOfService() {
		changeOperationalStatus(OperationalStatus.OUT_OF_SERVICE);
    }

    // changes the operational status of the bus fleet unit
    public void changeOperationalStatus(OperationalStatus newStatus) {

        if (newStatus == null) {
            		throw new IllegalArgumentException("Operational status cannot be null.");
        }

        switch (newStatus) {
            case ACTIVE -> {
                administrativeStatus = AdministrativeStatus.ACTIVE;
                availability = VehicleAvailability.AVAILABLE;
            }
            case INACTIVE -> administrativeStatus = AdministrativeStatus.INACTIVE;
            case UNDER_MAINTENANCE -> {
                administrativeStatus = AdministrativeStatus.ACTIVE;
                availability = VehicleAvailability.MAINTENANCE;
            }
            case OUT_OF_SERVICE -> {
                administrativeStatus = AdministrativeStatus.ACTIVE;
                availability = VehicleAvailability.OUT_OF_SERVICE;
            }
        }
        this.availabilityEffectiveAt = Instant.now();
        synchronizeOperationalStatus();
        this.updatedAt = Instant.now();
    }

    // archives the bus fleet unit
    public void archive() {

        if (archivedAt != null) {
            return;
        }

    Instant now = Instant.now();

    this.archivedAt = now;
    this.updatedAt = now;
    }

    // restores the bus fleet unit from the archive
    public void unarchive() {

        if (archivedAt == null) {
        						return;
        }

        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    // update fields
    public void updateDetails(
				            String unitNumber,
				            String plateNumber,
				            short capacity) {

    requireCapacity(capacity);
    this.unitNumber = unitNumber;
    this.plateNumber = plateNumber;
    this.capacity = capacity;
    this.updatedAt = Instant.now();
    }

    private static void requireCapacity(short capacity) {
        if (capacity < 1 || capacity > 80) {
            throw new IllegalArgumentException("Capacity must be between 1 and 80 passengers.");
        }
    }

    // state queries
    
    // return true if the bus fleet unit has been archived
    public boolean isArchived() {
					        return archivedAt != null;
    }

     // return true if the bus fleet is both considered as non-archived and
     // operationally available for dispatch.
    public boolean isAvailableForDispatch() {
								        return !isArchived()
								        && administrativeStatus == AdministrativeStatus.ACTIVE
                                        && availability == VehicleAvailability.AVAILABLE;
    }

    public void changeAdministrativeStatus(AdministrativeStatus status) {
        this.administrativeStatus = java.util.Objects.requireNonNull(status);
        synchronizeOperationalStatus();
        this.updatedAt = Instant.now();
    }

    public void changeAvailability(VehicleAvailability availability, Instant effectiveAt) {
        this.availability = java.util.Objects.requireNonNull(availability);
        this.availabilityEffectiveAt = java.util.Objects.requireNonNull(effectiveAt);
        synchronizeOperationalStatus();
        this.updatedAt = Instant.now();
    }

    private void synchronizeOperationalStatus() {
        this.operationalStatus = administrativeStatus == AdministrativeStatus.INACTIVE
                ? OperationalStatus.INACTIVE
                : switch (availability) {
                    case AVAILABLE -> OperationalStatus.ACTIVE;
                    case MAINTENANCE -> OperationalStatus.UNDER_MAINTENANCE;
                    case OUT_OF_SERVICE -> OperationalStatus.OUT_OF_SERVICE;
                    default -> OperationalStatus.INACTIVE;
                };
    }

    public AdministrativeStatus getAdministrativeStatus() {
        return administrativeStatus;
    }

    public VehicleAvailability getAvailability() {
        return availability;
    }

    public Instant getAvailabilityEffectiveAt() {
        return availabilityEffectiveAt;
    }

    // getters
    public Long getId() {
		return id;
    }

    public String getUnitNumber() {
		return unitNumber;
    }

    public String getPlateNumber() {
		return plateNumber;
    }

    public VehicleType getVehicleType() {
		return vehicleType;
    }

    public Short getCapacity() {
		return capacity;
    }

    public OperationalStatus getOperationalStatus() {
		return operationalStatus;
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
