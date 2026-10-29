package com.transit.arctransit.dispatch;

public record DispatchAssignmentView(
        Long id,
        String dispatchDate,
        Long fleetUnitId,
        String fleetUnitNumber,
        Long driverId,
        String driverName,
        Long routeId,
        String routeCode,
        String scheduledDeparture,
        String dispatchStatus,
        Long routeVersionId,
        String scheduledArrival,
        String actualDeparture,
        String actualArrival,
        String routeVersionEffectiveFrom,
        String routeVersionEffectiveTo,
        boolean legacyRouteSnapshot,
        Long scheduleId) {

    public DispatchAssignmentView(Long id, String dispatchDate, Long fleetUnitId,
                                  String fleetUnitNumber, Long driverId, String driverName,
                                  Long routeId, String routeCode, String scheduledDeparture,
                                  String dispatchStatus) {
        this(id, dispatchDate, fleetUnitId, fleetUnitNumber, driverId, driverName,
                routeId, routeCode, scheduledDeparture, dispatchStatus,
                null, null, null, null, null, null, true, null);
    }
}
