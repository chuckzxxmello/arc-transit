package com.transit.arctransit.reconciliation;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
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
    OperationalExceptionView acknowledgeException(@NotNull @Positive Long exceptionId);

    /** Transitions ACKNOWLEDGED -> INVESTIGATING. */
    OperationalExceptionView startInvestigation(@NotNull @Positive Long exceptionId);

    /** Transitions to RESOLVED with notes. */
    OperationalExceptionView resolveException(@NotNull @Positive Long exceptionId, @NotBlank @Size(max = 500) @SafeText(multiline = true) String notes);

    /** Transitions to DISMISSED with notes. */
    OperationalExceptionView dismissException(@NotNull @Positive Long exceptionId, @NotBlank @Size(max = 500) @SafeText(multiline = true) String notes);

    /** Retrieves summary metrics of open exceptions. */
    ExceptionSummaryView getExceptionSummary();

    /** Searches all operational exceptions with pagination. */
    Page<OperationalExceptionView> searchExceptions(Pageable pageable);
}
