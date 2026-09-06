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
    }
}