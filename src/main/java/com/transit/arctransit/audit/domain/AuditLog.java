package com.transit.arctransit.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// persistent append-only audit record

@Entity
@Table(
        name = "audit_logs",
        schema = "arc"
)
@org.hibernate.annotations.Immutable
public class AuditLog {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "action_name",
            nullable = false,
            length = 100
    )
    private String actionName;

    @Column(
            name = "entity_name",
            nullable = false,
            length = 100
    )
    private String entityName;

    @Column(
            name = "entity_id",
            nullable = false
    )
    private Long entityId;

    @Column(
            name = "performed_by",
            nullable = false,
            length = 100
    )
    private String performedBy;

    @Column(
            name = "timestamp",
            nullable = false
    )
    private Instant timestamp;

    @Column(
            name = "details",
            columnDefinition = "TEXT"
    )
    private String details;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    // JPA

    protected AuditLog() {
    }

    // create audit record

    public AuditLog(
            String actionName,
            String entityName,
            Long entityId,
            String performedBy,
            String details) {

        this(actionName, entityName, entityId, performedBy, details, "SUCCESS");
    }

    public AuditLog(String actionName, String entityName, Long entityId,
                    String performedBy, String details, String outcome) {
        this.actionName = actionName;
        this.entityName = entityName;
        this.entityId = entityId;
        this.performedBy = performedBy;
        this.timestamp = Instant.now();
        this.details = details;
        this.outcome = outcome;
    }

    // getters

    public Long getId() {
        return id;
    }

    public String getActionName() {
        return actionName;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetails() {
        return details;
    }
}