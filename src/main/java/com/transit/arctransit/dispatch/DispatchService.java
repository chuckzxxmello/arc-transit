package com.transit.arctransit.dispatch;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service contract for Dispatch and Assignment.
 *
 * This service validates cross-module business rules:
 * - Fleet unit must be ACTIVE and not archived
 * - Driver must be ACTIVE with a valid (non-expired) license
 * - Route must be ACTIVE and not archived
 * - No overlapping assignments for the same bus or driver
 */
public interface DispatchService {

    /** Creates a new dispatch assignment after validating all business rules. */
    DispatchAssignmentView createAssignment(@Valid CreateDispatchCommand command);

    /** Transitions SCHEDULED -> IN_PROGRESS. */
    DispatchAssignmentView startTrip(Long assignmentId);

    /** Transitions IN_PROGRESS -> COMPLETED. */
    DispatchAssignmentView completeTrip(Long assignmentId);

    /** Transitions to CANCELLED. */
    DispatchAssignmentView cancelAssignment(Long assignmentId);

    /** Searches dispatch assignments. */
    Page<DispatchAssignmentView> searchAssignments(DispatchQuery query, Pageable pageable);
}
