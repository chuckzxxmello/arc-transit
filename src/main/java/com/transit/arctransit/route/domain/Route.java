package com.transit.arctransit.route.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.ArrayList;
import java.util.List;

// fixed route and its ordered stops
@Entity
@Table(name = "routes", schema = "arc")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "route_code",
            length = 30,
            nullable = false,
            unique = true
    )
    private String routeCode;

    @Column(
            name = "route_name",
            length = 150,
            nullable = false
    )
    private String routeName;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "route_status",
            length = 20,
            nullable = false
    )
    private RouteStatus routeStatus;

    @Column(
            name = "estimated_duration_minutes"
    )
    private Integer estimatedDurationMinutes;

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

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JoinColumn(
            name = "route_id",
            nullable = false
    )
    @OrderBy("stopSequence ASC")
    private List<RouteStop> stops =
            new ArrayList<>();

    // JPA
    protected Route() {
    }

    // create route
    public Route(
            String routeCode,
            String routeName,
            String description,
            Integer estimatedDurationMinutes) {

        this.routeCode = routeCode;
        this.routeName = routeName;
        this.description = description;
        this.estimatedDurationMinutes =
                estimatedDurationMinutes;

        this.routeStatus =
                RouteStatus.ACTIVE;

        Instant now =
                Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    // change route status
    public void changeRouteStatus(
            RouteStatus newStatus) {

        if (newStatus == null) {

            throw new IllegalArgumentException(
                    "Route status cannot be null."
            );
        }

        this.routeStatus = newStatus;
        this.updatedAt = Instant.now();
    }

    // archive route
    public void archive() {

        if (archivedAt != null) {
            return;
        }

        Instant now =
                Instant.now();

        this.archivedAt = now;
        this.updatedAt = now;
    }

    // restore route
    public void unarchive() {

        if (archivedAt == null) {
            return;
        }

        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    // archive state
    public boolean isArchived() {
        return archivedAt != null;
    }

    // add route stop
    public void addStop(
            RouteStop stop) {

        if (stop == null) {

            throw new IllegalArgumentException(
                    "Route stop cannot be null."
            );
        }

        stops.add(stop);
        this.updatedAt = Instant.now();
    }

    // replace all route stops
    public void replaceStops(
            List<RouteStop> newStops) {

        if (newStops == null) {
            throw new IllegalArgumentException(
                    "Route stops cannot be null."
            );
        }

        stops.clear();
        stops.addAll(newStops);
        this.updatedAt = Instant.now();
    }

    // update route details
    public void updateDetails(
            String routeCode,
            String routeName,
            String description,
            Integer estimatedDurationMinutes) {

        this.routeCode = routeCode;
        this.routeName = routeName;
        this.description = description;
        this.estimatedDurationMinutes =
                estimatedDurationMinutes;

        this.updatedAt = Instant.now();
    }

    // getters
    public Long getId() {
        return id;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getDescription() {
        return description;
    }

    public RouteStatus getRouteStatus() {
        return routeStatus;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
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

    public List<RouteStop> getStops() {
        return stops;
    }
}