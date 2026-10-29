package com.transit.arctransit.dispatch;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for Dispatch
public interface DispatchService {

    Page<DispatchAssignmentView> searchReportCandidates(
            @jakarta.validation.constraints.Size(max = 200)
            @com.transit.arctransit.common.validation.SafeText String filter, Pageable pageable);

    DispatchAssignmentView getAssignment(@NotNull @Positive Long id);

    java.util.List<DispatchEventView> getHistory(@NotNull @Positive Long assignmentId);


    // create a new dispatch assignment
    DispatchAssignmentView createAssignment(
            @NotNull @Valid CreateDispatchCommand command
    );

    // start scheduled trip
    DispatchAssignmentView startTrip(
            @NotNull @Positive Long assignmentId
    );

    // complete an in-progress trip
    DispatchAssignmentView completeTrip(
            @NotNull @Positive Long assignmentId
    );

    // cancel scheduled or in-progress assignment
    DispatchAssignmentView cancelAssignment(
            @NotNull @Positive Long assignmentId
    );

    // search active dispatch assignments
    Page<DispatchAssignmentView> searchAssignments(
            @Valid DispatchQuery query,
            Pageable pageable
    );

    // dashboard count
    long countCompletedTrips();

    // archive completed or cancelled assignment
    void archiveAssignment(@NotNull @Positive Long id);

    // restore archived assignment
    void unarchiveAssignment(@NotNull @Positive Long id);

    void hardDeleteAssignment(@NotNull @Positive Long id);

    // search archived assignments
    Page<DispatchAssignmentView> searchArchivedAssignments(
            Pageable pageable
    );
}
