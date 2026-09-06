package com.transit.arctransit.route.domain;

// operational status of a route
public enum RouteStatus {

    // route is available for dispatch
    ACTIVE,

    // route exists but is not currently operational
    INACTIVE
}