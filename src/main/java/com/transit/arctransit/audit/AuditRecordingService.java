package com.transit.arctransit.audit;

/**
 * Public API for the Audit module.
 * Other modules call this to record important business actions.
 */
public interface AuditRecordingService {

    /**
     * Records a business action.
     *
     * @param actionName The name of the action (e.g., "DISPATCH_STARTED")
     * @param entityName The name of the domain entity (e.g., "DispatchAssignment")
     * @param entityId   The ID of the domain entity
     * @param details    Optional human-readable details
     */
    void recordAction(String actionName, String entityName, Long entityId, String details);
}
