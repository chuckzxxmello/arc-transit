package com.transit.arctransit.reporting;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.Instant;

@ConfigurationProperties(prefix = "arc.reporting")
public record ReportingProperties(Duration agingAfter, Duration staleAfter) {

    public ReportingProperties {
        if (agingAfter == null) {
            agingAfter = Duration.ofMinutes(30);
        }
        if (staleAfter == null) {
            staleAfter = Duration.ofHours(2);
        }
        if (agingAfter.isNegative() || agingAfter.isZero()
                || staleAfter.compareTo(agingAfter) <= 0) {
            throw new IllegalArgumentException("Report freshness thresholds must be positive and ordered.");
        }
    }

    public String freshness(Instant effectiveAt, Instant now) {
        Duration age = Duration.between(effectiveAt, now);
        if (age.compareTo(staleAfter) >= 0) {
            return "STALE";
        }
        return age.compareTo(agingAfter) >= 0 ? "AGING" : "CURRENT";
    }
}
