package com.transit.arctransit.dispatch;

// resolved dispatch assignment view
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
        String dispatchStatus

) {
}