package com.transit.arctransit.dispatch;

/**
 * Permits only Success and Failed implementations for exhaustive
 * pattern-matched handling.
 */
public sealed interface DispatchResult permits DispatchResult.Success, DispatchResult.Failed {

    record Success(DispatchAssignmentView assignmentView) implements DispatchResult {
    }

    record Failed(String reason) implements DispatchResult {
    }
}
