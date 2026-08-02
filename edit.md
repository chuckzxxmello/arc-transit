# Arc Transit System - Implementation Log

## Runtime and Build Configuration

The new repository has been configured and verified with:

- Java 21 as the project compilation and runtime baseline;
- Spring Boot 4.1.0;
- Maven Wrapper for reproducible local builds;
- Vaadin 25.2.3;
- PostgreSQL running through Docker Compose;
- Flyway for version-controlled database migrations;
- Spring Data JPA for later persistence mapping;
- Spring Modulith 2.1.0;
- JUnit 5 and Spring Boot testing support.

Verified commands include:

```cmd
mvnw.cmd validate
mvnw.cmd clean test
mvnw.cmd spring-boot:run
mvnw.cmd dependency:tree -Dincludes=org.springframework.modulith
```

The project has successfully:

- compiled with Java 21;
- loaded the Spring application context;
- executed the Maven test suite without test failures;
- started as an executable Spring Boot application;
- rendered the first Vaadin dashboard route;
- connected to PostgreSQL;
- applied a Flyway migration;
- verified the intended Spring Modulith package boundaries.

# Detailed Implementation Log

## Monday — July 13, 2026
- Created and tested the first working draft template of Spring Boot project. 
- Installed and configured the required development tools. 
  - PostgreSQL 17
  - Java JDK 21
  - Antigravity IDE
  - Docker Desktop (and the Debian container: postgres:17-bookworm)
  - `# debian.sh --arch 'amd64' out/ 'bookworm' '@1783900800'`

## Wednesday — July 22, 2026
- Created Arc Transit System project using Spring Initializr.
- Configured the project with Java 21, Spring Boot 4.1.0, Maven, and the Maven Wrapper.
- Used a temporary project-specific JAVA_HOME configuration (e.g. set cmd command to switch to JDK 21, so the project runs on Java 21) without changing the permanent system-wide Java version. Verified that Java, javac, Maven, and the Maven Wrapper were using the expected development environment.
  - `set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"`
  - `set "PATH=%JAVA_HOME%\bin;%PATH%"`
  to verify:
  - `mvnw.cmd -version`
  - `javac -version`
- Initialized the local Git repository for the new Arc Transit System project.
  - `https://github.com/chuckzxxmello/arc-transit.git`
- Ran the generated Spring Boot automated test and confirmed that the application context loaded successfully with no test failures.
  - `mvnw.cmd -version`
  - `mvnw.cmd clean test`

## Thursday — July 23, 2026
- Added the Vaadin dependencies on pom.xml
  - `<vaadin.version>25.2.3</vaadin.version>` (check pom.xml for the other configurations)
- Created a simple dashboard route to establish a server-side rendered user interface
  - `arctransit\analytics\ui\DashboardView.java`
- Recorded the progress via edit.md and README.md to ensure development process are tracked.
- Added the PostegreSQL dependencies on the pom.xml and configured the PostgreSQL to run on a Docker Desktop container via Docker Compose, and successfully connected the Java application to the database on port 5433 & 5432 (the other port for the running Spring Boot application, but the web still uses localhost: 8080).
  - `<groupId>org.postgresql</groupId>`
  - `docker-compose.yaml`
- Configured Flyway-managed database schemas (e.g. V1_create_fleet_units.sql), establishing a strict, version-controlled migration that will avoid unpredictable Hibernate auto-ddl behaviors in production environments.
- Implemented Spring Modulith dependencies and established the modular modulith architecture folder layout in the codebase, strictly isolating the application into distinct, cohesive business capability packages (e.g., auth, fleet, analytics).
  - `<groupId>org.springframework.modulith</groupId>` (-starter-test and -starter-core)
- Designed and implemented the database SQL schema specifically for authentication and user roles, implementing PostegreSQL constraints for lowercase uniqueness and fixed enum-style role mappings on the database.
  - `V2__create_authentication_tables.sql`
- Created and debugged the initial working authentication login component, ensuring the Vaadin interface correctly interfaces with the Spring Security dependency
  - `<groupId>org.springframework.boot</groupId>`
  - `<artifactId>spring-boot-starter-security</artifactId>`
- Updated edit.md status to reflect the completion and verification of the entire development process.

## Saturday — July 25, 2026 (pushed the project on July 26, 2026 at 1:30 AM)
- Implemented the DevAdminSeeder to automatically bootstrap the default admin account securely via JdbcTemplate when the dev Spring profile is active, removing unsafe SQL seed scripts from the migration history.
  - `arc-transit\src\main\java\com\transit\arctransit\auth\security\DevAdminSeeder.java`
- Refactored the Authentication module's public API to utilize immutable Java 21 records, for thread safety and eliminating mutable DTO boilerplate.
  - `arc-transit\src\main\java\com\transit\arctransit\auth\UserView.java`
  - `public record UserView (String username, String displayName, String email, String accountStatus, Set<String> roles) {}`
- Modernized the AppUser domain entities to enforce strict @Enumerated(EnumType.STRING) constraints for AccountStatus, completely mitigating the risk of raw string typos at runtime.
  - `@Enumerated(EnumType.STRING)`
  - `@Column(name = "account_status", length = 20, nullable = false)`
  - `private AccountStatus accountStatus;`
- Fixed critical authentication logic in AppUserDetailsService to accurately evaluate account locking and disabling using null-safe Java Enum equality comparisons.
- Implemented strict Role-Aware navigation across the Vaadin frontend by protecting the UserAdministrationView with Jakarta's @RolesAllowed("SYSTEM_ADMIN") annotation, successfully blocking unauthorized operations staff.
  - `arc-transit\src\main\java\com\transit\arctransit\auth\security\AppUserDetailsService.java`
  - `arc-transit\src\main\java\com\transit\arctransit\auth\ui\UserAdministrationView.java`
- Authored highly isolated unit tests utilizing Mockito to conclusively verify that the Spring Security login rejection rules for locked and disabled accounts function flawlessly without requiring the full Spring context.
  - `arc-transit\src\test\java\com\transit\arctransit\AuthenticationTests.java`
- Established a shared cross-cutting common module containing standard @ResponseStatus HTTP exception handlers to automate REST error translation.
  - `arc-transit\src\main\java\com\transit\arctransit\common\exception:`
  - `ResourceNotFoundException.java`
  - `BusinessConflictException.java`
- Resolved Spring Modulith architectural boundary violations by formally declaring the common package as an OPEN module via package-info.java, legally permitting exception sharing across isolated business modules.
  - `arc-transit\src\main\java\com\transit\arctransit\common\package-info.java:`
  - `@org.springframework.modulith.ApplicationModule(displayName = "Common Shared Library", type = org.springframework.modulith.ApplicationModule.Type.OPEN)`
- Ran automated test and successfully executed the Maven test suite (cmd -> mvnw clean test), achieving BUILD SUCCESS.
- Updated edit.md status to reflect the completion and verification of the entire development process.

---

# Repository Checkpoints

The following database-related commits were visible in the local Git history
during the rebuild:

```text
ea07047 connect java application to the postgres database
f72ac57 chore: configure Flyway-managed database schema
```

The latest implementation batch contained:

- the first Fleet Flyway migration;
- Spring Modulith dependency management;
- the nine application-module descriptors;
- the move of `DashboardView` into `analytics.ui`;
- the new `ModularityTests`;
- updated datasource and schema configuration.

The exact final commit identifier for that latest batch should be recorded here
after confirming it with:

```cmd
git log --oneline -5
```

Do not record a commit as completed until it exists locally and has been pushed
successfully.

# Current Verified Status

## Week 5

The following Week 5 outputs are implemented and verified:

- Java 21 and Maven Wrapper;
- Spring Boot 4.1.0 project;
- Vaadin application shell;
- PostgreSQL Docker service;
- application-to-database connection;
- Flyway-managed `arc` schema;
- first Fleet migration;
- nine Spring Modulith business modules;
- automated module-boundary verification;
- successful Maven test suite.

Week 5 may be marked **Completed** after the latest implementation commit is
confirmed in Git and pushed.

**Current Limitations**

The following items are not yet implemented in the manual rebuild:

- Spring Security dependency and configuration;
- `roles`, `app_users`, and `user_roles` database migrations;
- seeded `SYSTEM_ADMIN` and `OPERATIONS_STAFF` roles;
- a real application user or development-only administrator seed;
- BCrypt password handling;
- Vaadin login view;
- logout behavior;
- session-based authentication;
- role-aware navigation;
- access-denied handling;
- authentication and authorization audit events;
- `audit_log` persistence;
- Fleet JPA entity and repository;
- Fleet application service;
- Fleet Vaadin create, update, search, archive, and status workflows;
- Fleet integration tests;
- manual PostgreSQL inspection of all newly created table columns and
  constraints, unless separately recorded.

## Week 6

Verified and **Completed**:

- V1 Fleet migration exists and can be replayed;
- V2 authentication migration exists and can be replayed;
- fixed `SYSTEM_ADMIN` and `OPERATIONS_STAFF` role definitions are migrated;
- Spring Security and Vaadin security components are present;
- PostgreSQL-backed user lookup components are present;
- BCrypt password encoding infrastructure is present;
- a Vaadin login route is present;
- the Spring application context loads;
- the Spring Modulith verification test passes;
- the Maven test command completes with `BUILD SUCCESS`;
- **authentication Java implementation** (completed with `AppUser` records and enums);
- **administrator bootstrap** (completed with `DevAdminSeeder`);
- **runtime login and logout** (completed with Vaadin security and DashboardView logout button);
- **role-aware navigation** (completed using `@RolesAllowed("SYSTEM_ADMIN")` on the new `UserAdministrationView`);
- **account-state rejection** (completed inside `AppUserDetailsService` using `AccountStatus`);
- **authentication tests** (completed via `AuthenticationTests.java` ensuring locked/disabled states are handled properly).

Not yet implemented in the manual rebuild:

- Audit persistence and business-action recording.

---

## Saturday — August 2, 2026

- Added the `spring-boot-starter-validation` dependency to pom.xml to enable Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Valid`) on command records, ensuring that validation annotations on Java records are actually enforced at runtime instead of being silently ignored.
  - `<groupId>org.springframework.boot</groupId>`
  - `<artifactId>spring-boot-starter-validation</artifactId>`
  - Source: https://docs.spring.io/spring-boot/reference/io/validation.html

- Created the V3 Flyway migration (`V3__create_drivers.sql`) for the `arc.drivers` table, implementing detailed human-readable comments for every column and named database constraints (`ck_`, `ux_`, `pk_`) covering uppercase normalization, non-blank checks, license type validation, and employment status enums. Followed the exact commenting and constraint patterns established by V1 (fleet_units) and V2 (authentication).

- Created the V4 Flyway migration (`V4__create_routes_and_stops.sql`) with two tables: `arc.routes` (parent) and `arc.route_stops` (child). The child table implements ordered stops using a `stop_sequence` column with unique constraints on `(route_id, stop_sequence)` and `(route_id, stop_name)` to prevent duplicate ordering and duplicate stop names per route. The foreign key uses `ON DELETE CASCADE` for parent-child cleanup.

- Created the V5 Flyway migration (`V5__create_dispatch_assignments.sql`) for the `arc.dispatch_assignments` table. Implemented partial unique indexes (`ux_dispatch_fleet_unit_schedule`, `ux_dispatch_driver_schedule`) using PostgreSQL's `WHERE dispatch_status <> 'CANCELLED'` syntax to prevent double-booking a bus or driver at the same date and time while allowing cancelled assignments to be replaced.
  - Source: https://www.postgresql.org/docs/17/indexes-partial.html

- Implemented the complete Fleet module with full JPA persistence, following the same architectural patterns established by the Auth module:
  - Domain layer: `FleetUnit.java` entity with `@Enumerated(EnumType.STRING)` for `VehicleType` and `OperationalStatus`, `@Version` for optimistic locking, soft-delete via `archivedAt`, and domain methods for status transitions (`activate()`, `deactivate()`, `markUnderMaintenance()`, `markOutOfService()`, `archive()`).
  - Domain enums: `VehicleType.java` (BUS), `OperationalStatus.java` (ACTIVE, INACTIVE, UNDER_MAINTENANCE, OUT_OF_SERVICE).
  - Repository: `FleetUnitRepository.java` extending `JpaRepository` with derived query methods (`findByUnitNumber`, `findByPlateNumber`, `findByArchivedAtIsNull`).
  - Public API: `FleetUnitView`, `FleetUnitSummaryView`, `CreateFleetUnitCommand`, `UpdateFleetUnitCommand`, `FleetUnitQuery` (immutable Java 21 records).
  - Service interface: `FleetManagementService.java` (Modulith public API boundary).
  - Service implementation: `AppFleetManagementService.java` with input normalization (uppercase, trimmed), uniqueness validation, `@PreAuthorize` method-level security, and `@Transactional` management.
  - Vaadin view: `FleetManagementView.java` at route `/fleet` with Grid, Dialog-based create/edit forms, status change ComboBox, and archive button.
  - Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a14935 (EnumType.STRING)
  - Source: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html (JpaRepository)
  - Source: https://vaadin.com/docs/latest/components/grid (Grid component)
  - Source: https://vaadin.com/docs/latest/components/dialog (Dialog component)

- Implemented the complete Driver module with full JPA persistence and license expiry checking:
  - Domain layer: `Driver.java` entity with license expiry detection via `isLicenseExpired()` method comparing `licenseExpiryDate` against `LocalDate.now()`.
  - Domain enums: `EmploymentStatus.java` (ACTIVE, INACTIVE, SUSPENDED, TERMINATED), `LicenseType.java` (PROFESSIONAL, NON_PROFESSIONAL).
  - Repository: `DriverRepository.java` with derived query methods.
  - Public API: `DriverView`, `DriverSummaryView` (includes `licenseExpired` boolean flag), `CreateDriverCommand`, `UpdateDriverCommand`, `DriverQuery`.
  - Service: `AppDriverManagementService.java` with employee number and license number uniqueness validation.
  - Vaadin view: `DriverManagementView.java` at route `/drivers` with a visual red "EXPIRED" badge for drivers with expired licenses, DatePicker for license expiry input.
  - Source: https://vaadin.com/docs/latest/components/date-picker (DatePicker)

- Implemented the complete Route module with parent-child JPA relationship for ordered stops:
  - Domain layer: `Route.java` entity with `@OneToMany(cascade = ALL, orphanRemoval = true)` for `RouteStop` child entities, `@OrderBy("stopSequence ASC")` for correct loading order, and `replaceStops()` method for atomic stop replacement.
  - Domain layer: `RouteStop.java` child entity with `stopSequence` ordering and `estimatedArrivalMinutes`.
  - Repository: `RouteRepository.java`.
  - Public API: `RouteView`, `RouteSummaryView`, `RouteStopView`, `CreateRouteCommand` (with nested `StopEntry` record), `UpdateRouteCommand`.
  - Service: `AppRouteManagementService.java`.
  - Vaadin view: `RouteManagementView.java` at route `/routes` with dynamic stop list management in the create/edit dialogs.
  - Source: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a1005 (orphanRemoval)

- Implemented the complete Dispatch module with cross-module validation and lifecycle state machine:
  - Updated `dispatch/package-info.java` with `allowedDependencies = {"fleet", "driver", "route", "common"}` to formally declare cross-module references following Spring Modulith best practices instead of using `Type.OPEN`.
  - Domain layer: `DispatchAssignment.java` entity using FK-by-ID pattern (plain `Long` IDs instead of `@ManyToOne`) to respect module boundaries. State machine methods (`startTrip()`, `completeTrip()`, `cancel()`) with explicit transition validation.
  - Domain enum: `DispatchStatus.java` (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) with documented valid transitions.
  - Repository: `DispatchAssignmentRepository.java` with overlap-checking query methods.
  - Public API: `DispatchAssignmentView` (includes resolved display names), `CreateDispatchCommand`, `DispatchQuery`.
  - Service: `AppDispatchService.java` with 5-step validation: (1) fleet unit ACTIVE, (2) driver ACTIVE + license valid, (3) route ACTIVE, (4) no bus overlap, (5) no driver overlap. Uses public service interfaces for cross-module entity lookup.
  - Vaadin view: `DispatchView.java` at route `/dispatch` with ComboBox dropdowns for active entities and context-aware action buttons.
  - Source: https://docs.spring.io/spring-modulith/reference/fundamentals.html (allowedDependencies)
  - Source: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html (@PreAuthorize)
  - Source: https://vaadin.com/docs/latest/components/combo-box (ComboBox)

---

## Week 7/8 — Current Verified Position

Verified and **Completed** (Phases 1–5):

- `spring-boot-starter-validation` dependency added;
- V3 drivers migration with full constraints;
- V4 routes and route_stops migration with parent-child relationship;
- V5 dispatch_assignments migration with partial unique indexes;
- Fleet module: entity, enums, repository, service, Vaadin CRUD view;
- Driver module: entity, enums, repository, service, Vaadin CRUD view with license expiry indicator;
- Route module: entity with @OneToMany ordered stops, repository, service, Vaadin CRUD view with dynamic stops;
- Dispatch module: entity with FK-by-ID, state machine, cross-module validation, Vaadin dispatch view;
- `dispatch/package-info.java` updated with `allowedDependencies`.

Not yet implemented (Phase 6/7 — remaining manual tasks):

- Unit tests for Fleet, Driver, Route, and Dispatch services;
- Console debug output verification;
- `mvnw clean test` BUILD SUCCESS confirmation;
- Audit persistence and business-action recording.

## Sunday — August 2, 2026 (UI & Security Fixes)

- Removed the hardcoded admin credential insertion from `V2__create_users.sql`.
- Created `AdminUserInitializer.java` utilizing Spring's `@Value` property injection (`arc.security.admin.username`) to dynamically create the `SYSTEM_ADMIN` user on first boot securely, supporting environment variable overrides for production.
- Resolved Spring Modulith architectural boundary violations caused by cyclic dependencies. Moved `MainLayout.java` to `com.transit.arctransit.common.ui.MainLayout` and refactored Vaadin `SideNavItem` definitions to use raw string routes instead of class imports, completely severing the module cycle.
- Fixed UI layout defects in `MainLayout`: centered the global search bar using `FlexComponent.JustifyContentMode.CENTER` and prevented the clock wrapping with `white-space: nowrap`.
- Refactored `DashboardView.java` to replace static placeholders with live integration:
  - Built real Vaadin `Grid` tables connected to `RouteManagementService` and `FleetManagementService`.
  - Replaced the map placeholder with an OpenStreetMap `IFrame` focused on the Cavite region (`bbox=120.7,14.1,121.1,14.5`).
  - Developed a Vaadin `SplitLayout` slide-out detail panel replacing the right sidebar, appearing only when a route or bus row is clicked.
- Implemented `UserAdministrationView.java` restricted exclusively to `SYSTEM_ADMIN`. Included full CRUD with `CreateUserCommand` to allow administrators to dynamically create `OPERATIONS_STAFF` testing accounts securely leveraging `PasswordEncoder`.
- Integrated `hardDeleteUnit`, `hardDeleteDriver`, and `hardDeleteRoute` methods into their respective modules, exposed via new "Permanently Delete" action buttons strictly in the `ArchiveView` interface for `SYSTEM_ADMIN` roles only. Backed by Vaadin `ConfirmDialog` prompts to prevent accidental deletions.
