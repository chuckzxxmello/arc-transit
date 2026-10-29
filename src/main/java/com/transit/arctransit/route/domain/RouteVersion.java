package com.transit.arctransit.route.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route_versions", schema = "arc")
public class RouteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "route_code", nullable = false, length = 30)
    private String routeCode;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(length = 500)
    private String description;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "legacy_snapshot", nullable = false)
    private boolean legacySnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "route_version_id", nullable = false)
    @OrderBy("stopSequence ASC")
    private List<RouteVersionStop> stops = new ArrayList<>();

    protected RouteVersion() {
    }

    public RouteVersion(Route route, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveFrom == null || (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom))) {
            throw new IllegalArgumentException("Route version requires a positive effective date interval.");
        }
        this.routeId = route.getId();
        this.routeCode = route.getRouteCode();
        this.routeName = route.getRouteName();
        this.description = route.getDescription();
        this.estimatedDurationMinutes = route.getEstimatedDurationMinutes();
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = Instant.now();
        for (RouteStop stop : route.getStops()) {
            stops.add(new RouteVersionStop(stop.getStopName(), stop.getStopSequence(),
                    stop.getEstimatedArrivalMinutes()));
        }
    }

    public void closeAt(LocalDate date) {
        if (date == null || !date.isAfter(effectiveFrom)
                || (effectiveTo != null && date.isAfter(effectiveTo))) {
            throw new IllegalArgumentException("Invalid route version closing date.");
        }
        this.effectiveTo = date;
    }

    public boolean isEffectiveOn(LocalDate date) {
        return date != null && !date.isBefore(effectiveFrom)
                && (effectiveTo == null || date.isBefore(effectiveTo));
    }

    public Long getId() { return id; }
    public Long getRouteId() { return routeId; }
    public String getRouteCode() { return routeCode; }
    public String getRouteName() { return routeName; }
    public String getDescription() { return description; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public boolean isLegacySnapshot() { return legacySnapshot; }
    public List<RouteVersionStop> getStops() { return List.copyOf(stops); }
}
