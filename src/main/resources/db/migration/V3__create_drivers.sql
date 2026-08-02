/*
 * Table:
 *      drivers
 *
 * Important rules:
 * - Drivers are operational staff records, not application user accounts.
 *   A driver does not automatically receive a login account.
 *
 *   Source: V2__create_authentication_tables.sql header comment
 *   "Drivers are operational records and do not automatically receive accounts."
 *
 * - Employee numbers remain unique across active and archived records.
 * - License numbers remain unique across active and archived records.
 * - Java/JPA manages the application timestamps (created_at, updated_at).
 * - Driver records are archived (soft-delete) instead of hard-deleted.
 * - JPA optimistic locking uses the version column.
 *
 * License model:
 *   The system stores a freeform license number, a license type
 *   (PROFESSIONAL or NON_PROFESSIONAL), and a license expiry date.
 *   The Java application service validates that a driver's license
 *   is not expired before allowing dispatch assignments.
 *
 * Employment lifecycle:
 *   ACTIVE     -> The driver is available for dispatch assignments.
 *   INACTIVE   -> The driver is registered but not currently available.
 *   SUSPENDED  -> The driver is temporarily barred from assignments.
 *   TERMINATED -> The driver's employment has ended permanently.
 */

CREATE TABLE arc.drivers (

    /*
     * Internal database identifier for a driver record.
     *
     * PostgreSQL generates this value automatically.
     * Application code does not choose the ID.
     */
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * Internal employee identifier assigned by HR or fleet management.
     *
     * Ex:
     *      DRV-001
     *      DRV-002
     *
     * This value must remain unique across active and archived records.
     */
    employee_number VARCHAR(30) NOT NULL,

    /*
     * Driver's first name.
     */
    first_name VARCHAR(80) NOT NULL,

    /*
     * Driver's last name (surname / family name).
     */
    last_name VARCHAR(80) NOT NULL,

    /*
     * Government-issued driver's license number.
     *
     * This value must remain unique across active and archived records
     * because one physical license cannot belong to two different
     * driver records.
     */
    license_number VARCHAR(50) NOT NULL,

    /*
     * Classification of the driver's license.
     *
     * PROFESSIONAL:
     *   Licensed to operate public utility vehicles (buses).
     *
     * NON_PROFESSIONAL:
     *   Standard personal license. A driver holding only this type
     *   should not be assigned to bus dispatch, but the restriction
     *   is enforced by the Java application service, not by this
     *   database constraint.
     */
    license_type VARCHAR(30) NOT NULL,

    /*
     * Date when the driver's license expires.
     *
     * The Java application service must check this date before
     * creating a dispatch assignment. An expired license should
     * prevent the driver from being dispatched.
     *
     * Stored as a DATE (no time component) because license
     * expiry is measured in calendar days.
     */
    license_expiry_date DATE NOT NULL,

    /*
     * Optional contact phone number.
     * NULL means no contact number was supplied.
     */
    contact_number VARCHAR(30),

    /*
     * Current employment status of the driver.
     *
     * A newly registered driver begins as ACTIVE so they are
     * immediately available for dispatch assignment.
     */
    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    /*
     * Time when the driver record was first created.
     *
     * The application supplies this value through JPA auditing
     * when the entity is first saved. PostgreSQL stores it as
     * TIMESTAMPTZ.
     */
    created_at TIMESTAMPTZ NOT NULL,

    /*
     * Time when the driver record was last changed.
     *
     * JPA auditing assigns this value on first save and
     * replaces it after later updates.
     */
    updated_at TIMESTAMPTZ NOT NULL,

    /*
     * Soft-delete/archive timestamp.
     *
     * if archived_at is NULL:
     *      -> The driver record is active (not archived).
     *
     * if archived_at is NOT NULL:
     *      -> The driver has been archived but remains stored
     *         for dispatch and audit history references.
     */
    archived_at TIMESTAMPTZ,

    /*
     * JPA optimistic-lock version.
     *
     * Detects conflicting edits to the same driver record.
     * Application code must not manually change this value.
     */
    version BIGINT NOT NULL DEFAULT 0,

    /*
     * Primary-key constraint.
     */
    CONSTRAINT pk_drivers
        PRIMARY KEY (id),

    /*
     * Prevents two driver rows from using the same employee number.
     * This constraint applies to every row, including archived rows.
     */
    CONSTRAINT ux_drivers_employee_number
        UNIQUE (employee_number),

    /*
     * Prevents two driver rows from using the same license number.
     * This constraint applies to every row, including archived rows,
     * preserving a permanent link between one license and one driver.
     */
    CONSTRAINT ux_drivers_license_number
        UNIQUE (license_number),

    /*
     * Employee numbers must be uppercase, trimmed, and nonblank.
     *
     * The Java service should normalize input before saving.
     * These checks provide the final database safety boundary.
     */
    CONSTRAINT ck_drivers_employee_number_normalized
        CHECK (employee_number = BTRIM(UPPER(employee_number))),

    CONSTRAINT ck_drivers_employee_number_not_blank
        CHECK (BTRIM(employee_number) <> ''),

    /*
     * First name must not be blank.
     */
    CONSTRAINT ck_drivers_first_name_not_blank
        CHECK (BTRIM(first_name) <> ''),

    /*
     * Last name must not be blank.
     */
    CONSTRAINT ck_drivers_last_name_not_blank
        CHECK (BTRIM(last_name) <> ''),

    /*
     * License number must be trimmed and nonblank.
     */
    CONSTRAINT ck_drivers_license_number_not_blank
        CHECK (BTRIM(license_number) <> ''),

    /*
     * License type must be one of the accepted classifications.
     */
    CONSTRAINT ck_drivers_license_type
        CHECK (
            license_type IN (
                'PROFESSIONAL',
                'NON_PROFESSIONAL'
            )
        ),

    /*
     * Employment status must be one of the accepted lifecycle states.
     */
    CONSTRAINT ck_drivers_employment_status
        CHECK (
            employment_status IN (
                'ACTIVE',
                'INACTIVE',
                'SUSPENDED',
                'TERMINATED'
            )
        )
);
