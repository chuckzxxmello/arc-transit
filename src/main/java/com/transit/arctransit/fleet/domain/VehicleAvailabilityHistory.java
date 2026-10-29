package com.transit.arctransit.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vehicle_availability_history", schema = "arc")
public class VehicleAvailabilityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fleet_unit_id", nullable = false, updatable = false)
    private Long fleetUnitId;

    @Column(name = "previous_availability", length = 30, updatable = false)
    private String previousAvailability;

    @Column(name = "availability", nullable = false, length = 30, updatable = false)
    private String availability;

    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, length = 100, updatable = false)
    private String recordedBy;

    @Column(name = "source_type", nullable = false, length = 30, updatable = false)
    private String sourceType;

    @Column(name = "verification_status", nullable = false, length = 30, updatable = false)
    private String verificationStatus;

    @Column(name = "reason", nullable = false, length = 2000, updatable = false)
    private String reason;

    protected VehicleAvailabilityHistory() {
    }

    public VehicleAvailabilityHistory(
            Long fleetUnitId, String previousAvailability, String availability,
            Instant effectiveAt, Instant recordedAt, String recordedBy,
            String sourceType, String reason) {
        this.fleetUnitId = fleetUnitId;
        this.previousAvailability = previousAvailability;
        this.availability = availability;
        this.effectiveAt = effectiveAt;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
        this.sourceType = sourceType;
        this.verificationStatus = "UNVERIFIED";
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getPreviousAvailability() { return previousAvailability; }
    public String getAvailability() { return availability; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public String getSourceType() { return sourceType; }
    public String getVerificationStatus() { return verificationStatus; }
    public String getReason() { return reason; }
}