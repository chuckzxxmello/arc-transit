package com.transit.arctransit.dispatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "dispatch_events", schema = "arc")
public class DispatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    protected DispatchEvent() {
    }

    public DispatchEvent(Long assignmentId, DispatchStatus previousStatus,
                         DispatchStatus newStatus, String actor, Instant effectiveAt) {
        this.assignmentId = assignmentId;
        this.previousStatus = previousStatus == null ? null : previousStatus.name();
        this.newStatus = newStatus.name();
        this.actor = actor;
        this.effectiveAt = effectiveAt;
        this.recordedAt = Instant.now();
        this.sourceType = "STAFF_RECORDED";
    }

    public Long getAssignmentId() { return assignmentId; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getActor() { return actor; }
    public String getSourceType() { return sourceType; }
}
