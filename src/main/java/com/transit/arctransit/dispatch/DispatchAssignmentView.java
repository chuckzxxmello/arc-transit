package com.transit.arctransit.dispatch;

/**
 * Immutable view of a dispatch assignment for frontend consumption.
 *
 * Contains resolved display names for fleet unit, driver, and route
 * (not just IDs) so the Vaadin view can display human-readable labels.
 */
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
) {}
