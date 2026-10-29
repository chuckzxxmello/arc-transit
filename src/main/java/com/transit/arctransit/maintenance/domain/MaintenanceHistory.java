package com.transit.arctransit.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "maintenance_history", schema = "arc")
public class MaintenanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maintenance_record_id", nullable = false, updatable = false)
    private Long maintenanceRecordId;

    @Column(name = "previous_status", length = 30, updatable = false)
    private String previousStatus;

    @Column(nullable = false, length = 30, updatable = false)
    private String status;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, length = 100, updatable = false)
    private String recordedBy;

    @Column(nullable = false, length = 2000, updatable = false)
    private String reason;

    protected MaintenanceHistory() {
    }

    public MaintenanceHistory(Long maintenanceRecordId, String previousStatus,
            String status, Instant recordedAt, String recordedBy, String reason) {
        this.maintenanceRecordId = maintenanceRecordId;
        this.previousStatus = previousStatus;
        this.status = status;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getPreviousStatus() { return previousStatus; }
    public String getStatus() { return status; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public String getReason() { return reason; }
}