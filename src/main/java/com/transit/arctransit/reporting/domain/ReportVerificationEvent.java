package com.transit.arctransit.reporting.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "report_verification_events", schema = "arc")
public class ReportVerificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, updatable = false)
    private Long reportId;

    @Column(name = "previous_status", length = 30, updatable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 30, updatable = false)
    private String newStatus;

    @Column(name = "recorded_by", nullable = false, length = 100, updatable = false)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "reason", nullable = false, length = 2000, updatable = false)
    private String reason;

    protected ReportVerificationEvent() {
    }

    public ReportVerificationEvent(Long reportId, String previousStatus, String newStatus,
            String actor, String reason) {
        this.reportId = reportId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.recordedBy = actor;
        this.recordedAt = Instant.now();
        this.reason = reason;
    }

    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getReason() { return reason; }
}
