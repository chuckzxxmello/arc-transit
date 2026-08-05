/*
 * Table:
 *      audit_logs
 *
 * Stores a persistent history of important business actions performed
 * within the application. Serves as a compliance and troubleshooting ledger.
 */

CREATE TABLE arc.audit_logs (

    /* Internal identifier */
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /* 
     * The business action that was performed.
     * Ex: "FLEET_UNIT_REGISTERED", "DISPATCH_COMPLETED"
     */
    action_name VARCHAR(100) NOT NULL,

    /* 
     * The domain entity type this action affected.
     * Ex: "FleetUnit", "DispatchAssignment"
     */
    entity_name VARCHAR(100) NOT NULL,

    /* 
     * The ID of the affected domain entity.
     * Stored as BIGINT but intentionally NOT a foreign key to prevent
     * constraint violations if records are hard-deleted in the future.
     */
    entity_id BIGINT NOT NULL,

    /* 
     * The username of the actor who performed the action.
     * "SYSTEM" if performed by a background job.
     */
    performed_by VARCHAR(100) NOT NULL,

    /* When the action occurred */
    timestamp TIMESTAMPTZ NOT NULL,

    /* Optional human-readable details about the action */
    details TEXT,

    /* Primary key constraint */
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

/* Index for faster searching by entity and action type */
CREATE INDEX idx_audit_logs_entity ON arc.audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON arc.audit_logs (timestamp);
