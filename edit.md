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

---

## Monday — August 3, 2026 (Week 7 & 8 Deliverables)

- Implemented the complete cross-module Audit Logging System to act as an append-only ledger for business actions:
  - Created the V8 Flyway migration (`V8__create_audit_logs.sql`) for the `arc.audit_logs` table. Intentionally defined `entity_id` as a plain `BIGINT` (no foreign key) to prevent constraint violations when audited entities are later hard-deleted. Added performance indexes `idx_audit_logs_entity` and `idx_audit_logs_timestamp`.
  - Domain layer: `AuditLog.java` entity representing a single action (e.g., "FLEET_UNIT_REGISTERED").
  - Repository: `AuditLogRepository.java`.
  - Service interface: `AuditRecordingService.java` acting as the public API boundary for the `audit` module.
  - Service implementation: `AppAuditRecordingService.java` utilizing `SecurityContextHolder.getContext().getAuthentication()` to automatically attribute actions to the active authenticated user, or falling back to "SYSTEM" for background tasks.
  - Integration: Constructor-injected `AuditRecordingService` into all primary module services (`AppRouteManagementService`, `AppFleetManagementService`, `AppDriverManagementService`, `AppDispatchService`) to record actions at the service boundary.

- Finalized the Dispatch and Incident schema and persistence layers:
  - Created the V7 Flyway migration (`V7__add_dispatch_archive.sql`) to append an `archived_at` TIMESTAMPTZ column to `arc.dispatch_assignments`, enabling soft-deletion of completed or cancelled trips so active views aren't cluttered.
  - Corrected previous Flyway migration sequence collisions by formally standardizing the ordering: V6 (`incidents`), V7 (`dispatch_assignments` alter), and V8 (`audit_logs`).
  - Resolved subtle transaction timing issues across integration tests by explicitly distinguishing `save()` vs `saveAndFlush()` within `AppUserAdministrationService`, ensuring immediate database synchronization before assertions.

- Dashboard & System Verification:
  - Integrated the dashboard summary cards in `DashboardView` with real data, directly querying module services for active fleet units, active routes, recent incidents, and total completed dispatch trips. Made the dashboard cards clickable to route to their respective views.
  - Developed the `StartupDiagnosticRunner` (an `ApplicationRunner` in the `analytics` module) to provide runtime proof of module interactions during application boot by counting domain entities across modules and logging them.

- Testing, Security, and Environment Hardening:
  - Created comprehensive JUnit 5 and Mockito unit tests for `AppFleetManagementService`, `AppDriverManagementService`, `AppRouteManagementService`, and `AppDispatchService`, verifying all cross-module validations, unique constraints, and state transitions.
  - Engineered `AuthDebugIT.java`, an `@SpringBootTest` integration test directly exercising `AppUserDetailsService.loadUserByUsername()`. Verified that the database role mappings successfully translate into Spring Security `GrantedAuthority` objects (e.g., `ROLE_SYSTEM_ADMIN`).
  - Adjusted PostgreSQL connection credentials specifically tailored for the integration test environment context to ensure robust automated CI/CD pipeline execution.
  - Ran `mvnw clean test` confirming that all unit tests and context loads succeed under Java 21, achieving BUILD SUCCESS and closing out the remaining Week 8 structural deliverables.

---

## Thursday — August 6, 2026 (JDK 21 Optimization & Architectural Mapping)

- **JDK 21 Virtual Threads Integration (Project Loom):**
  - Added `spring.threads.virtual.enabled=true` to `application.properties` to switch Spring Boot's web server (Tomcat) and Vaadin background tasks from standard platform OS threads to JDK 21 lightweight Virtual Threads (`Thread.ofVirtual()`).
  - *Why this matters:* Traditional Java threads map 1:1 with heavy OS threads (consuming ~1MB RAM per thread). Virtual Threads are managed by the JVM at near-zero memory cost, allowing the transit system to handle thousands of concurrent operations (such as live bus GPS pings or Vaadin push updates) without running out of thread-pool capacity.
  - File updated: [application.properties](file:///c:/projects/arc-transit/src/main/resources/application.properties)

- **JDK 21 Sealed Result Interfaces & Exhaustive Pattern Matching:**
  - Created `DispatchResult.java` using Java 21's `sealed interface` feature, restricting its implementations strictly to two record types: `DispatchResult.Success` (holding `DispatchAssignmentView`) and `DispatchResult.Failed` (holding a String `reason`).
  - *Why this matters:* Instead of throwing runtime exceptions for expected business failures or returning generic boolean flags, callers can process results using Java 21 pattern-matched `switch` expressions. Because the interface is `sealed`, the compiler enforces that every possible outcome is handled without needing fallback `default` blocks or runtime null-checks.
  - File created: [DispatchResult.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/dispatch/DispatchResult.java)

- **System Design & Module Architecture Tree Mapping:**
  - Conducted a full audit of the codebase to streamline domain value objects and address class proliferation across domain packages. Cited Domain-Driven Design (DDD) principles by Eric Evans/Martin Fowler on Aggregate Root encapsulation and Spring Modulith API package boundaries.
  - Built a comprehensive, visual Tree Node Map in `implementation_plan.md` documenting every directory, package, database migration, domain entity, Spring Data repository, service implementation, record DTO, and Vaadin UI view across all modules (`auth`, `fleet`, `driver`, `route`, `dispatch`, `audit`, `analytics`, `maintenance`, `common`), explaining the explicit responsibility of each class and interface.
  - File created: [implementation_plan.md](file:///c:/projects/arc-transit/implementation_plan.md)

- **JDK 21 Build & Modularity Verification:**
  - Executed Maven test suite explicitly pointing `JAVA_HOME` to JDK 21 (`C:\Program Files\Java\jdk-21.0.11`).
  - Confirmed 100% pass rate across 38 unit and modularity tests:
    - `ModularityTests` — Verified all Spring Modulith 2.1.0 package boundaries and dependency rules (`allowedDependencies = {"fleet", "driver", "route", "common"}`).
    - `AppDispatchServiceTest` (9 tests) — Validated 5-step cross-module dispatch validation rules and lifecycle state machine transitions.
    - `AppFleetManagementServiceTest` (9 tests) — Verified bus unit registration, status updates, and soft archiving.
    - `AppDriverManagementServiceTest` (8 tests) — Verified license expiry detection and employee number uniqueness checks.
    - `AppRouteManagementServiceTest` (7 tests) — Verified route creation and atomic stop sequence reordering.
    - `AuthenticationTests` (4 tests) — Verified BCrypt password matching and account lock/disable checks.

- **Online Documentation References Used:**
  - Spring Boot Virtual Threads: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.virtual-threads
  - Oracle Java 21 Virtual Threads Specification: https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html
  - Oracle Java 21 Sealed Classes & Interfaces: https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html
  - Java 21 Pattern Matching for switch: https://docs.oracle.com/en/java/javase/21/language/pattern-matching-switch.html
  - Java 21 Records Language Guide: https://docs.oracle.com/en/java/javase/21/language/records.html
  - Martin Fowler — Value Objects: https://martinfowler.com/bliki/ValueObject.html
  - Martin Fowler — DDD Aggregate Patterns: https://martinfowler.com/bliki/DDD_Aggregate.html
  - Baeldung — Implementing DDD Value Objects in Java: https://www.baeldung.com/java-ddd-value-objects
  - Spring Modulith Fundamentals & Boundaries: https://docs.spring.io/spring-modulith/reference/fundamentals.html
  - Spring Modulith Event-Driven Architecture: https://docs.spring.io/spring-modulith/reference/events.html

- **OWASP Top 10 Security Hardening & Performance Optimizations:**
  - **OWASP A01 (Broken Access Control):** Annotated [ArchiveView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/analytics/ui/ArchiveView.java) with `@RolesAllowed({"SYSTEM_ADMIN", "OPERATIONS_STAFF"})` to ensure soft-deleted archive data is protected from unauthorized access.
  - **OWASP A05 (Security Misconfiguration & Clickjacking Protection):** Configured HTTP Security Headers in [SecurityConfig.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/security/SecurityConfig.java) (`frameOptions(SAMEORIGIN)` and `contentTypeOptions()`) to prevent clickjacking attacks on Vaadin UI frames and MIME-sniffing.
  - **OWASP A07 (Password Complexity Validation):** Enhanced [CreateUserCommand.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/CreateUserCommand.java) with a `@Pattern` regular expression enforcing strong passwords (minimum 8 characters, uppercase, lowercase, number, and special character).
  - **OWASP A09 (Security Event Audit Logging):** Created [AuthenticationEventListener.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/security/AuthenticationEventListener.java) listening to Spring Security `AuthenticationSuccessEvent` and `AbstractAuthenticationFailureEvent`. Automatically logs `USER_LOGIN_SUCCESS` and `USER_LOGIN_FAILED` (with target username and failure reason) to `arc.audit_logs`.
  - **Network & Database Pool Tuning:** Enabled Gzip HTTP response compression for Vaadin JS/CSS static bundles (`server.compression.enabled=true`) and optimized HikariCP connection pool settings (`maximum-pool-size=10`, `minimum-idle=5`, `connection-timeout=20000`) in [application.properties](file:///c:/projects/arc-transit/src/main/resources/application.properties).
  - **Verification:** Ran `mvnw test` with JDK 21 `JAVA_HOME`, confirming 100% pass rate across all 38 unit and modularity tests (including `ModularityTests` verifying `AuthenticationEventListener` within the `auth` module boundary).

- **Dispatch Archiving & Dashboard Integration:**
  - Added Archived Dispatches tab to [ArchiveView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/analytics/ui/ArchiveView.java) bound to `DispatchService.searchArchivedAssignments()` with an "Unarchive" action button (`unarchiveAssignment`).
  - Confirmed status lifecycle flow in `DispatchView.java` (`SCHEDULED -> IN_PROGRESS -> COMPLETED` / `CANCELLED`).

- **Dynamic Route Stop Deletion:**
  - Added a trash icon "Remove Stop" button (`Button removeBtn`) to each `StopFormEntry` row in [RouteManagementView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/route/ui/RouteManagementView.java), enabling staff to delete added or existing stops directly from the dialog form before saving.

- **Staff Account Management & Role Promotion:**
  - Added `deleteUser(String username)` method to [UserAdministrationService.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/UserAdministrationService.java) and [AppUserAdministrationService.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/application/AppUserAdministrationService.java) with `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`.
  - Added "Delete Account" (with `ConfirmDialog` warning) and "Update Role" buttons to [UserAdministrationView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/auth/ui/UserAdministrationView.java), allowing administrators to permanently remove staff accounts or promote `OPERATIONS_STAFF` to `SYSTEM_ADMIN`.

- **Custom Error Handling & Access Denied Redirects:**
  - Created [CustomAccessDeniedView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/common/ui/CustomAccessDeniedView.java) implementing `HasErrorParameter<AccessDeniedException>` to render a friendly access denied screen with a "Go Back to Dashboard" button whenever an unauthorized user (e.g. `OPERATIONS_STAFF`) attempts to access restricted routes like `/admin/users`, `/fleet`, or `/archive`.
  - Created [CustomNotFoundView.java](file:///c:/projects/arc-transit/src/main/java/com/transit/arctransit/common/ui/CustomNotFoundView.java) implementing `HasErrorParameter<NotFoundException>` to render an "Invalid Page — Go Back to Dashboard" screen for invalid or non-existent URLs.

- **Full Build & Compilation Verification:**
  - Resolved `RolesAllowed` imports and property accessors (`fleetUnitNumber()`, `dispatchStatus()`) in `ArchiveView.java` and `RouteManagementView.java`.
  - Executed `mvnw test-compile` with JDK 21 `JAVA_HOME`, achieving **BUILD SUCCESS** across all 97 main source files and 8 test files with zero errors under release target 21.







