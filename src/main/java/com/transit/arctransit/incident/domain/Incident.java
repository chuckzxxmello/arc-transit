package com.transit.arctransit.incident.domain;

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

// operational incident entity
@Entity
@Table(
        name = "incidents",
        schema = "arc"
)
public class Incident {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "title",
            nullable = false
    )
    private String title;

    @Column(
            name = "description",
            length = 1000
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "severity",
            nullable = false,
            length = 50
    )
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "affected_service_type",
            length = 30
    )
    private AffectedServiceType affectedServiceType;

    @Column(
            name = "affected_entity_id"
    )
    private Long affectedEntityId;

    @Column(
            name = "reported_by",
            length = 100
    )
    private String reportedBy;

    @Column(
            name = "reported_at",
            nullable = false
    )
    private Instant reportedAt;

    @Column(
            name = "acknowledged_at"
    )
    private Instant acknowledgedAt;

    @Column(
            name = "resolved_at"
    )
    private Instant resolvedAt;

    @Column(
            name = "resolution_notes",
            columnDefinition = "TEXT"
    )
    private String resolutionNotes;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(
            name = "archived_at"
    )
    private Instant archivedAt;

    @Version
    private Long version;

    // JPA

    protected Incident() {
    }

    // create incident

    public Incident(
            String title,
            String description,
            IncidentSeverity severity,
            AffectedServiceType affectedServiceType,
            Long affectedEntityId,
            String reportedBy) {

        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = IncidentStatus.REPORTED;
        this.affectedServiceType = affectedServiceType;
        this.affectedEntityId = affectedEntityId;
        this.reportedBy = reportedBy;

        Instant now =
                Instant.now();

        this.reportedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // acknowledge incident
    public void acknowledge() {

        requireStatus(
                IncidentStatus.REPORTED,
                "acknowledge"
        );

        Instant now =
                Instant.now();

        this.status =
                IncidentStatus.ACKNOWLEDGED;

        this.acknowledgedAt = now;
        this.updatedAt = now;
    }

    // start investigation

    public void startInvestigation() {

        requireStatus(
                IncidentStatus.ACKNOWLEDGED,
                "start investigation"
        );

        this.status =
                IncidentStatus.INVESTIGATING;

        this.updatedAt =
                Instant.now();
    }

    // resolve incident

    public void resolve(
            String resolutionNotes) {

        requireStatus(
                IncidentStatus.INVESTIGATING,
                "resolve"
        );

        Instant now =
                Instant.now();

        this.status =
                IncidentStatus.RESOLVED;

        this.resolutionNotes =
                resolutionNotes;

        this.resolvedAt = now;
        this.updatedAt = now;
    }

    // close incident

    public void close() {

        requireStatus(
                IncidentStatus.RESOLVED,
                "close"
        );

        this.status =
                IncidentStatus.CLOSED;

        this.updatedAt =
                Instant.now();
    }

    // validate lifecycle transition

    private void requireStatus(
            IncidentStatus expectedStatus,
            String operation) {

        if (status != expectedStatus) {

            throw new IllegalStateException(
                    "Cannot "
                            + operation
                            + ": current status is "
                            + status
                            + ", expected "
                            + expectedStatus
            );
        }
    }

    // getters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public AffectedServiceType getAffectedServiceType() {
        return affectedServiceType;
    }

    public Long getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
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