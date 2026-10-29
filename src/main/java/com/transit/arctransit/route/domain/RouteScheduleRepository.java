package com.transit.arctransit.route.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RouteScheduleRepository extends JpaRepository<RouteSchedule, Long> {

    List<RouteSchedule> findByRouteIdOrderByPlannedDepartureAscNameAsc(Long routeId);

    boolean existsByRouteIdAndNameAndEffectiveFrom(Long routeId, String name, LocalDate effectiveFrom);
}
