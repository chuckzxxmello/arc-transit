package com.transit.arctransit.route.domain;

import jakarta.persistence.*;

/**
 * An ordered stop along a specific route.
 *
 * Maps to the arc.route_stops table created by V4__create_routes_and_stops.sql.
 *
 * The stop_sequence determines the visiting order:
 *   1 = first stop (origin terminal)
 *   2 = second stop
 *   N = last stop (destination terminal)
 *
 * This entity is managed as a child of Route through JPA's
 * @OneToMany(cascade = ALL, orphanRemoval = true) relationship.
 * When a RouteStop is removed from Route's collection, JPA
 * automatically deletes the database row.
 *
 * Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a1005
 * (Ctrl+F: orphanRemoval)
 */
@Entity
@Table(name = "route_stops", schema = "arc")
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_name", length = 150, nullable = false)
    private String stopName;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    /** JPA requires a protected no-args constructor. */
    protected RouteStop() {
    }

    public RouteStop(String stopName, int stopSequence, Integer estimatedArrivalMinutes) {
        this.stopName = stopName;
        this.stopSequence = stopSequence;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getStopName() { return stopName; }
    public Integer getStopSequence() { return stopSequence; }
    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }

    // --- Setters for reordering ---

    public void setStopSequence(Integer stopSequence) { this.stopSequence = stopSequence; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }
}
