package com.transit.arctransit.audit;

// public audit API used by other modules
public interface AuditRecordingService {

    // record an important business or security action
    void recordAction(
    		
            String actionName,
            String entityName,
            Long entityId,
            String details
    );
}