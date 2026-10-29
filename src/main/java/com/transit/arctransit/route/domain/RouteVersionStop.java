package com.transit.arctransit.route.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_version_stops", schema = "arc")
public class RouteVersionStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_name", nullable = false, length = 150)
    private String stopName;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    protected RouteVersionStop() {
    }

    public RouteVersionStop(String stopName, Integer stopSequence, Integer estimatedArrivalMinutes) {
        this.stopName = stopName;
        this.stopSequence = stopSequence;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    public String getStopName() { return stopName; }
    public Integer getStopSequence() { return stopSequence; }
    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
}
