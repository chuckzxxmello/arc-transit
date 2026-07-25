/*
 * This table schema stores the master identity and administrative availability state
 * of each bus registered in the Arc Transit fleet.
 *
 * Important design rules:
 *      - Newly registered buses begin as INACTIVE.
 *      - Unit numbers and plate numbers remain unique even after archival.
 *      - Records are archived (soft delete) instead of permanently deleted (hard delete).
 *      - Java/JPA will manage created_at and updated_at.
 *      - JPA optimistic locking uses the version column.
 */

CREATE TABLE arc.fleet_units (

    /*
     * Internal database identifier for bus unit.
     *
     * PostgreSQL generates this value automatically.
     * Application code does not choose the ID.
     *
     * Declares id as the table's primary identifier.
     * IDENTITY generates the value.
     * PRIMARY KEY prevents duplicate or NULL IDs and allows
     * other tables to reference this fleet unit.
     */

    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * Internal fleet identifier used by staff.
     *
     * Ex:
     *      BUS-001
     *      BUS-002
     *
     * This value must remain unique across active and archived records.
     */
    unit_number VARCHAR(30) NOT NULL,

    /*
     * Vehicle registration plate number.
     *
     * This value remains unique across active and archived records,
     *  historical records always refer to one physical bus unit.
     */
    plate_number VARCHAR(20) NOT NULL,

    /*
     * Vehicle classification -> "BUS"
     *
     * The column remains in the schema so additional vehicle types
     *  may be introduced later through another Flyway migration.
     */
    vehicle_type VARCHAR(30) NOT NULL DEFAULT 'BUS',

    /*
     * Maximum supported passenger capacity.
     *
     * error fix -> the valid bus must have a passenger capacity greater than zero.
     */
    capacity SMALLINT NOT NULL,

    /*
     * Current operational status/availability of the bus unit.
     *
     * A newly created bus unit begins as "INACTIVE" so adding bus data
     * will not automatically make it available for dispatch.
     *
     * Allowed values:
     * - ACTIVE
     * - INACTIVE
     * - UNDER_MAINTENANCE
     * - OUT_OF_SERVICE
     */
    operational_status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',

    /*
    * Time when the fleet-unit record was first  (e.g. date created).
    *
    * The application will supply this value through JPA auditing
    * when the entity is first saved. PostgreSQL stores it as
    * TIMESTAMPTZ.
    *
    * A direct SQL insert must provide this value manually because
    * the database does not define a timestamp default.
    */
    created_at TIMESTAMPTZ NOT NULL,

    /*
    * Time when the fleet-unit record was last changed.
    *
    * JPA auditing will assign this value when the entity is first
    * saved and replace it after later updates.
    *
    * PostgreSQL does not update this column automatically because
    * the database has no update trigger for it.
    */
    updated_at TIMESTAMPTZ NOT NULL,

    /*
     * Soft-delete/archive timestamp.
     *
     * if the "TIMESTAMPTZ" is "NULL":
     *      -> The fleet unit is not archived.
     *
     * if the "TIMESTAMPTZ" is "NOT NULL":
     *      -> The fleet unit has been archived but remains stored for
     *          dispatch, incident, maintenance, and audit history.
     */
    archived_at TIMESTAMPTZ,

    /*
    * Detects conflicting edits to the same fleet-unit record.
    *
    * JPA reads the current version with the entity. When updating,
    * it verifies that the database still contains that version.
    *
    * If another transaction already updated the record, the versions
    * no longer match and JPA rejects the stale update.
    *
    * Application code must not manually change this value.
    */
    version BIGINT NOT NULL DEFAULT 0,

    /*
     * Primary-key constraint.
     */
    CONSTRAINT pk_fleet_units
        PRIMARY KEY (id),

    /*
    * Prevents two fleet-unit rows from using the same unit number.
    *
    * This constraint applies to every row, including archived rows,
    * because it has no condition that excludes archived records.
    */
    CONSTRAINT ux_fleet_unit_number
        UNIQUE (unit_number),

    /*
    * Prevents two fleet-unit rows from using the same plate number.
    *
    * The restriction also applies to archived rows, preserving a
    * permanent link between one plate number and one fleet record.
    */
    CONSTRAINT ux_fleet_plate_number
        UNIQUE (plate_number),

    /*
     * Unit numbers must already be uppercase, contain no surrounding
     * spaces, and contain at least one visible character.
     *
     * it is expected that Java service should normalize input before saving.
     * These checks provide the final database safety boundary.
     */
    CONSTRAINT ck_fleet_unit_number_normalized
    CHECK (unit_number = BTRIM(UPPER(unit_number))),

    CONSTRAINT ck_fleet_unit_number_not_blank
    CHECK (BTRIM(unit_number) <> ''),

    /*
    * Plate numbers must be uppercase, trimmed, and nonblank.
    * Formatting rules beyond that remain an application concern
    * because Philippine plate formats can vary.
    */
    CONSTRAINT ck_fleet_plate_number_normalized
    CHECK (plate_number = BTRIM(UPPER(plate_number))),

    CONSTRAINT ck_fleet_plate_number_not_blank
    CHECK (BTRIM(plate_number) <> ''),

    /*
     * keep as "BUS" only.
     */
    CONSTRAINT ck_fleet_vehicle_type
        CHECK (vehicle_type = 'BUS'),

    /*
     * Prevent zero and negative passenger capacity.
     */
    CONSTRAINT ck_fleet_capacity_positive
        CHECK (capacity > 0),

    /*
     * Reject unknown or incorrectly spelled operational states.
     *
     * The full process will be controlled by the Java FleetApplicationService.
     * This database constraint only checks if the
     *  stored values belong to the accepted "operational_status" list.
     */
    CONSTRAINT ck_fleet_operational_status
        CHECK (
            operational_status IN (
                'ACTIVE',
                'INACTIVE',
                'UNDER_MAINTENANCE',
                'OUT_OF_SERVICE'
            )
        )
);