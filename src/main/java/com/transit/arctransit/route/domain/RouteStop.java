package com.transit.arctransit.route.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ordered stop belonging to a route
@Entity
@Table(
        name = "route_stops",
        schema = "arc"
)
public class RouteStop {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "stop_name",
            length = 150,
            nullable = false
    )
    private String stopName;

    @Column(
            name = "stop_sequence",
            nullable = false
    )
    private Integer stopSequence;

    @Column(
            name = "estimated_arrival_minutes"
    )
    private Integer estimatedArrivalMinutes;

    // JPA
    protected RouteStop() {
    }

    // create route stop
    public RouteStop(
            String stopName,
            int stopSequence,
            Integer estimatedArrivalMinutes) {

        this.stopName = stopName;
        this.stopSequence = stopSequence;
        this.estimatedArrivalMinutes =
                estimatedArrivalMinutes;
    }

    // update stop data
    public void updateDetails(
            String stopName,
            int stopSequence,
            Integer estimatedArrivalMinutes) {

        this.stopName = stopName;
        this.stopSequence = stopSequence;
        this.estimatedArrivalMinutes =
                estimatedArrivalMinutes;
    }

    // getters
    public Long getId() {
        return id;
    }

    public String getStopName() {
        return stopName;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public Integer getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }
}