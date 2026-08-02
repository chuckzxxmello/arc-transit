package com.transit.arctransit.route.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A fixed route that buses follow in the Arc Transit network.
 *
 * Maps to the arc.routes table created by V4__create_routes_and_stops.sql.
 *
 * Design decisions:
 *
 * 1. @OneToMany(cascade = ALL, orphanRemoval = true) for route stops.
 *    When stops are added to or removed from the stops list, JPA
 *    automatically persists inserts and deletes without requiring
 *    a separate RouteStopRepository.
 *    Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a1005
 *    (Ctrl+F: CascadeType.ALL)
 *
 * 2. @OrderBy("stopSequence ASC") ensures stops are always loaded
 *    in the correct visiting order from the database.
 *    Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a14940
 *    (Ctrl+F: @OrderBy)
 *
 * 3. @JoinColumn(name = "route_id") places the foreign key on the
 *    route_stops table, matching the V4 migration schema.
 */
@Entity
@Table(name = "routes", schema = "arc")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", length = 30, nullable = false, unique = true)
    private String routeCode;

    @Column(name = "route_name", length = 150, nullable = false)
    private String routeName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_status", length = 20, nullable = false)
    private RouteStatus routeStatus;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    @OrderBy("stopSequence ASC")
    private List<RouteStop> stops = new ArrayList<>();

    /** JPA requires a protected no-args constructor. */
    protected Route() {
    }

    /**
     * Creates a new route with ACTIVE status.
     */
    public Route(String routeCode, String routeName, String description,
                 Integer estimatedDurationMinutes) {
        this.routeCode = routeCode;
        this.routeName = routeName;
        this.description = description;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.routeStatus = RouteStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // --- Domain Methods ---

    /** Changes the route operational status. */
    public void changeRouteStatus(RouteStatus newStatus) {
        this.routeStatus = newStatus;
        this.updatedAt = Instant.now();
    }

    /** Soft-deletes this route by setting the archive timestamp. */
    public void archive() {
        this.archivedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Restores this route from the archive. */
    public void unarchive() {
        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    /** Returns true when this route has not been archived. */
    public boolean isNotArchived() {
        return archivedAt == null;
    }

    /** Adds a stop to the route's ordered sequence. */
    public void addStop(RouteStop stop) {
        stops.add(stop);
        this.updatedAt = Instant.now();
    }

    /** Removes all stops and replaces them with a new list. */
    public void replaceStops(List<RouteStop> newStops) {
        stops.clear();
        stops.addAll(newStops);
        this.updatedAt = Instant.now();
    }

    /** Updates route details. */
    public void updateDetails(String routeCode, String routeName, String description,
                              Integer estimatedDurationMinutes) {
        this.routeCode = routeCode;
        this.routeName = routeName;
        this.description = description;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getRouteCode() { return routeCode; }
    public String getRouteName() { return routeName; }
    public String getDescription() { return description; }
    public RouteStatus getRouteStatus() { return routeStatus; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public List<RouteStop> getStops() { return stops; }
}
