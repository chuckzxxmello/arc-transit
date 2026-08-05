/*
 * Table:
 *      dispatch_assignments
 * 
 * Adds the archived_at column to support soft-deletion / manual archiving
 * of completed and cancelled dispatch assignments, ensuring active dispatch
 * views aren't cluttered.
 */

ALTER TABLE arc.dispatch_assignments
    ADD COLUMN archived_at TIMESTAMPTZ;
