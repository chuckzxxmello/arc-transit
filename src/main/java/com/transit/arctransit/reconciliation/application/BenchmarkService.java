package com.transit.arctransit.reconciliation.application;

import com.transit.arctransit.reconciliation.ExceptionSummaryView;
import com.transit.arctransit.reconciliation.ReconciliationService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// development-only reconciliation benchmark
@Service
@Profile("dev")
public class BenchmarkService {

    private final ReconciliationService reconciliationService;

    public BenchmarkService(
            ReconciliationService reconciliationService) {

        this.reconciliationService =
                reconciliationService;
    }

    public record BenchmarkResult(
            long durationMs,
            ExceptionSummaryView summary) {
    }

    // execute reconciliation and measure elapsed time
    public BenchmarkResult runBenchmark() {

        long start =
                System.nanoTime();

        reconciliationService.runReconciliation();

        ExceptionSummaryView summary =
                reconciliationService
                        .getExceptionSummary();

        long elapsedNanos =
                System.nanoTime()
                        - start;

        long durationMs =
                elapsedNanos / 1_000_000;

        return new BenchmarkResult(
                durationMs,
                summary
        );
    }
}