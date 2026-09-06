package com.transit.arctransit.reconciliation;

import java.util.Map;

/**
 * Summary statistics of open operational exceptions for the dashboard.
 */
public record ExceptionSummaryView(
        long totalOpen,
        Map<String, Long> countByType,
        Map<String, Long> countBySeverity
) {}
