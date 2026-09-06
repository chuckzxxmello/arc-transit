package com.transit.arctransit.reconciliation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service contract for the Reconciliation engine.
 */
public interface ReconciliationService {

    /** 
     * Runs the reconciliation engine to detect contradictions and stale states.
     * Generates OperationalExceptions for detected anomalies.
     */
    void runReconciliation();

    /** Transitions OPEN -> ACKNOWLEDGED. */
    OperationalExceptionView acknowledgeException(Long exceptionId);

    /** Transitions ACKNOWLEDGED -> INVESTIGATING. */
    OperationalExceptionView startInvestigation(Long exceptionId);

    /** Transitions to RESOLVED with notes. */
    OperationalExceptionView resolveException(Long exceptionId, String notes);

    /** Transitions to DISMISSED with notes. */
    OperationalExceptionView dismissException(Long exceptionId, String notes);

    /** Retrieves summary metrics of open exceptions. */
    ExceptionSummaryView getExceptionSummary();

    /** Searches all operational exceptions with pagination. */
    Page<OperationalExceptionView> searchExceptions(Pageable pageable);
}
