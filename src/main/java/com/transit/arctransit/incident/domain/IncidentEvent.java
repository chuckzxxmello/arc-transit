package com.transit.arctransit.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "incident_events", schema = "arc")
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Column(name = "previous_status", length = 50, updatable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 50, updatable = false)
    private String newStatus;

    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, length = 100, updatable = false)
    private String recordedBy;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reason;

    protected IncidentEvent() {
    }

    public IncidentEvent(Long incidentId, String previousStatus, String newStatus,
            Instant effectiveAt, String actor, String reason) {
        this.incidentId = incidentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.effectiveAt = effectiveAt;
        this.recordedAt = Instant.now();
        this.recordedBy = actor;
        this.reason = reason;
    }

    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public String getReason() { return reason; }
}
