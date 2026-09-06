package com.transit.arctransit.dispatch;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for Dispatch
public interface DispatchService {

    // create a new dispatch assignment
    DispatchAssignmentView createAssignment(
            @Valid CreateDispatchCommand command
    );

    // start scheduled trip
    DispatchAssignmentView startTrip(
            Long assignmentId
    );

    // complete an in-progress trip
    DispatchAssignmentView completeTrip(
            Long assignmentId
    );

    // cancel scheduled or in-progress assignment
    DispatchAssignmentView cancelAssignment(
            Long assignmentId
    );

    // search active dispatch assignments
    Page<DispatchAssignmentView> searchAssignments(
            DispatchQuery query,
            Pageable pageable
    );

    // dashboard count
    long countCompletedTrips();

    // archive completed or cancelled assignment
    void archiveAssignment(Long id);

    // restore archived assignment
    void unarchiveAssignment(Long id);

    // search archived assignments
    Page<DispatchAssignmentView> searchArchivedAssignments(
            Pageable pageable
    );
}