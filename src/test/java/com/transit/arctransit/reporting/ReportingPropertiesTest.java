package com.transit.arctransit.reporting;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReportingPropertiesTest {

    @Test
    void freshnessUsesEffectiveTimeAndExactConfiguredBoundaries() {
        ReportingProperties properties = new ReportingProperties(null, null);
        Instant now = Instant.parse("2026-10-15T12:00:00Z");
        assertEquals("CURRENT", properties.freshness(now.minusSeconds(1799), now));
        assertEquals("AGING", properties.freshness(now.minusSeconds(1800), now));
        assertEquals("AGING", properties.freshness(now.minusSeconds(7199), now));
        assertEquals("STALE", properties.freshness(now.minusSeconds(7200), now));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportingProperties(Duration.ZERO, Duration.ofHours(1)));
    }
}
