package com.transit.arctransit.reconciliation.domain;

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

// persistent operational exception

@Entity
@Table(
        name = "operational_exceptions",
        schema = "arc"
)
public class OperationalException {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "exception_type",
            nullable = false,
            length = 30
    )
    private ExceptionType exceptionType;

    @Column(
            name = "severity",
            nullable = false,
            length = 20
    )
    private String severity;

    @Column(
            name = "affected_entity_type",
            nullable = false,
            length = 30
    )
    private String affectedEntityType;

    @Column(
            name = "affected_entity_id",
            nullable = false
    )
    private Long affectedEntityId;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "exception_status",
            nullable = false,
            length = 30
    )
    private ExceptionStatus exceptionStatus;

    @Column(
            name = "detected_at",
            nullable = false
    )
    private Instant detectedAt;

    @Column(
            name = "acknowledged_by",
            length = 100
    )
    private String acknowledgedBy;

    @Column(
            name = "acknowledged_at"
    )
    private Instant acknowledgedAt;

    @Column(
            name = "resolved_by",
            length = 100
    )
    private String resolvedBy;

    @Column(
            name = "resolved_at"
    )
    private Instant resolvedAt;

    @Column(
            name = "resolution_notes",
            columnDefinition = "TEXT"
    )
    private String resolutionNotes;

    @Version
    private Long version;

    // JPA

    protected OperationalException() {
    }

    // create new exception

    public OperationalException(
            ExceptionType type,
            String severity,
            String entityType,
            Long entityId,
            String description) {

        this.exceptionType = type;
        this.severity = severity;
        this.affectedEntityType = entityType;
        this.affectedEntityId = entityId;
        this.description = description;
        this.exceptionStatus =
                ExceptionStatus.OPEN;
        this.detectedAt =
                Instant.now();
    }

    // acknowledge OPEN exception

    public void acknowledge(
            String username) {

        requireStatus(
                ExceptionStatus.OPEN,
                "acknowledge"
        );

        this.exceptionStatus =
                ExceptionStatus.ACKNOWLEDGED;

        this.acknowledgedBy =
                username;

        this.acknowledgedAt =
                Instant.now();
    }

    // start investigation

    public void startInvestigation() {

        requireStatus(
                ExceptionStatus.ACKNOWLEDGED,
                "start investigation"
        );

        this.exceptionStatus =
                ExceptionStatus.INVESTIGATING;
    }

    // resolve investigated exception

    public void resolve(
            String username,
            String notes) {

        requireStatus(
                ExceptionStatus.INVESTIGATING,
                "resolve"
        );

        this.exceptionStatus =
                ExceptionStatus.RESOLVED;

        this.resolvedBy =
                username;

        this.resolvedAt =
                Instant.now();

        this.resolutionNotes =
                notes;
    }

    // dismiss investigated exception

    public void dismiss(
            String username,
            String notes) {

        requireStatus(
                ExceptionStatus.INVESTIGATING,
                "dismiss"
        );

        this.exceptionStatus =
                ExceptionStatus.DISMISSED;

        this.resolvedBy =
                username;

        this.resolvedAt =
                Instant.now();

        this.resolutionNotes =
                notes;
    }

    // lifecycle guard

    private void requireStatus(
            ExceptionStatus expected,
            String operation) {

        if (exceptionStatus != expected) {

            throw new IllegalStateException(
                    "Cannot "
                            + operation
                            + ": current status is "
                            + exceptionStatus
                            + ", expected "
                            + expected
            );
        }
    }

    // getters

    public Long getId() {
        return id;
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAffectedEntityType() {
        return affectedEntityType;
    }

    public Long getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getDescription() {
        return description;
    }

    public ExceptionStatus getExceptionStatus() {
        return exceptionStatus;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }
}