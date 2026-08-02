package com.transit.arctransit.analytics.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incidents", schema = "arc")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    protected Incident() {
    }

    public Incident(String title, String description, String severity, String status) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = status;
        this.reportedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public Instant getReportedAt() { return reportedAt; }
}
