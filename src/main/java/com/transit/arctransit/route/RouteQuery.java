package com.transit.arctransit.route;

// search criteria for routes
public record RouteQuery(

        String routeOrNameFilter,
        String statusFilter

) {
}