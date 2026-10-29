package com.transit.arctransit.reporting.domain;

import com.transit.arctransit.common.exception.CommandValidationException;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "operational_reports", schema = "arc")
public class OperationalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ReportSourceType sourceType;

    @Column(name = "reported_by", nullable = false, length = 100)
    private String reportedBy;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_reason", length = 2000)
    private String verificationReason;

    @Version
    private Long version;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected OperationalReport() {
    }

    public OperationalReport(ReportSourceType sourceType, String reportedBy,
            Instant effectiveAt, String entityType, Long entityId, String description) {
        this.sourceType = sourceType;
        this.reportedBy = reportedBy;
        this.reportedAt = Instant.now();
        this.effectiveAt = effectiveAt;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public void verify(VerificationStatus status, String actor, String reason) {
        if (archivedAt != null) {
            throw new CommandValidationException("Restore this report before reviewing it.");
        }
        if (status == VerificationStatus.UNVERIFIED
                || status == verificationStatus
                || verificationStatus == VerificationStatus.CONFIRMED
                || verificationStatus == VerificationStatus.REJECTED) {
            throw new CommandValidationException("This report verification transition is not allowed.");
        }
        if (reason == null || reason.isBlank() || reason.length() > 2000) {
            throw new CommandValidationException("Verification reason is required and must not exceed 2000 characters.");
        }
        this.verificationStatus = status;
        this.verifiedBy = actor;
        this.verifiedAt = Instant.now();
        this.verificationReason = reason.trim();
    }

    public void archive() {
        if (archivedAt == null) archivedAt = Instant.now();
    }

    public void restore() {
        archivedAt = null;
    }

    public Long getId() { return id; }
    public ReportSourceType getSourceType() { return sourceType; }
    public String getReportedBy() { return reportedBy; }
    public Instant getReportedAt() { return reportedAt; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getVerificationReason() { return verificationReason; }
    public Instant getArchivedAt() { return archivedAt; }
}
