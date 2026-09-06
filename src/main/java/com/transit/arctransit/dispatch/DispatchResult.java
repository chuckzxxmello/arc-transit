package com.transit.arctransit.dispatch;

// result type for future explicit success/failure handling
public sealed interface DispatchResult
        permits DispatchResult.Success,
        DispatchResult.Failed {

    record Success(
            DispatchAssignmentView assignmentView
    ) implements DispatchResult {
    }

    record Failed(
            String reason
    ) implements DispatchResult {
    }
}