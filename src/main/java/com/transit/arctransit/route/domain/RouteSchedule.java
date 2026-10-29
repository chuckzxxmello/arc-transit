package com.transit.arctransit.route.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "route_schedules", schema = "arc")
public class RouteSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "planned_departure", nullable = false)
    private LocalTime plannedDeparture;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected RouteSchedule() {
    }

    public RouteSchedule(Long routeId, String name, LocalTime plannedDeparture,
                         LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (routeId == null || name == null || name.isBlank() || plannedDeparture == null
                || effectiveFrom == null || (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom))) {
            throw new IllegalArgumentException("Schedule requires a route, name, departure and positive effective interval.");
        }
        this.routeId = routeId;
        this.name = name;
        this.plannedDeparture = plannedDeparture;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getRouteId() { return routeId; }
    public String getName() { return name; }
    public LocalTime getPlannedDeparture() { return plannedDeparture; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public boolean isActive() { return active; }
}
