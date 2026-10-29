package com.transit.arctransit.reconciliation;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// configuration for reconciliation thresholds

@ConfigurationProperties(prefix = "arc.reconciliation")
public record ReconciliationProperties(

        // IN_PROGRESS dispatches older than this are stale

        Duration dispatchStalenessThreshold,

        // unresolved incidents older than this are stale

        Duration incidentResolutionDeadline

) {

    public ReconciliationProperties {

        if (dispatchStalenessThreshold == null) {
            dispatchStalenessThreshold =
                    Duration.ofHours(2);
        }

        if (incidentResolutionDeadline == null) {
            incidentResolutionDeadline =
                    Duration.ofHours(24);
        }
        if (dispatchStalenessThreshold.isNegative() || dispatchStalenessThreshold.isZero()
                || incidentResolutionDeadline.isNegative() || incidentResolutionDeadline.isZero()) {
            throw new IllegalArgumentException("Reconciliation thresholds must be positive.");
        }
    }
}