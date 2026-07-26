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

## Wednesday — July 23, 2026

### 1. Re-established the Spring Boot and Vaadin foundation

- Confirmed that Maven uses Java 21.
- Confirmed that the project inherits dependency and plugin management from
  Spring Boot 4.1.0.
- Added and verified the Vaadin 25.2.3 BOM.
- Added the Vaadin Spring Boot starter and required development dependencies.
- Configured the Vaadin Maven frontend build plugin.
- Verified that the application can start as an executable Spring Boot JAR.
- Created the first working `DashboardView`.
- Confirmed that a Vaadin route can render through the running application.

### 2. Created the local PostgreSQL environment

- Added a PostgreSQL Docker Compose service based on PostgreSQL 17 Bookworm.
- Configured the container with environment variables for:
  - database name;
  - application database user;
  - database password.
- Mapped local port `5433` to PostgreSQL container port `5432`.
- Added a named Docker volume so local PostgreSQL data survives container
  recreation unless the volume is deliberately deleted.
- Added a PostgreSQL health check using `pg_isready`.
- Kept the real database password outside the repository through a local
  `.env` file.
- Confirmed that the Docker Compose configuration is valid.
- Confirmed that the PostgreSQL container reaches a healthy state.
- Confirmed that the application database user can log in to the
  `arc_transit` database.

Representative verification commands:

```cmd
docker compose -f docker-compose.yaml config -q
docker compose up -d postgres
docker compose ps
docker compose exec postgres psql -U arc_application -d arc_transit
```

### 3. Connected Spring Boot to PostgreSQL

- Added the PostgreSQL JDBC driver as a runtime dependency.
- Added Spring Data JPA.
- Added Spring Boot Flyway support.
- Added `flyway-database-postgresql` so the current Flyway version can operate
  against PostgreSQL.
- Configured the application datasource to connect to:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/arc_transit
spring.datasource.username=arc_application
spring.datasource.password=${POSTGRES_PASSWORD}
```

- Kept the password external through the `POSTGRES_PASSWORD` environment
  variable.
- Configured Hibernate to validate the database schema instead of creating or
  modifying tables automatically:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This decision keeps Flyway as the single owner of physical database schema
changes.

### 4. Configured the Flyway-managed `arc` schema

- Configured Flyway to use the `arc` application schema.
- Corrected the configuration from the incorrect singular property
  `spring.flyway.schema` to the supported plural property
  `spring.flyway.schemas`.
- Set the same schema as the Flyway default schema.
- Configured Hibernate to map entities to the same `arc` schema.

Representative configuration:

```properties
spring.flyway.schemas=arc
spring.flyway.default-schema=arc
spring.jpa.properties.hibernate.default_schema=arc
```

- Re-ran the Spring application-context test after the datasource and schema
  corrections.
- Verified that the application connects to PostgreSQL and that Flyway can
  initialize without a datasource or schema error.

### 5. Finalized the first Fleet database decisions

Before writing the first migration, the Fleet master-record rules were reviewed
and finalized.

#### Optimistic locking

- The `version` column remains.
- New mutable records begin at version `0`.
- JPA will manage the version through optimistic locking.
- The value will not be manually edited by users.
- Stale updates will be rejected instead of silently overwriting a newer
  database state.

#### Timestamp ownership

- Java and Spring Data JPA auditing will assign `created_at` and `updated_at`.
- Java `Instant` values will represent UTC timestamps.
- PostgreSQL stores the values as `TIMESTAMPTZ`.
- Database timestamp triggers are not used.

#### Vehicle classification

- Arc Transit Version 1 accepts only `BUS`.
- The `vehicle_type` column remains in the table.
- A database check constraint rejects every value except `BUS`.
- The column is retained so future vehicle types can be introduced through a
  deliberate Flyway migration.

#### Fleet operational states

The accepted fleet-unit states are:

- `ACTIVE`
- `INACTIVE`
- `UNDER_MAINTENANCE`
- `OUT_OF_SERVICE`

A newly registered bus begins as `INACTIVE`. Creating its master record does not
automatically declare it ready for dispatch.

#### Capacity

- Capacity must be greater than zero.
- No unsupported maximum-capacity assumption has been added.

#### Archival and identifier reuse

- Fleet units are archived instead of physically deleted.
- Archived rows retain their unit number and plate number.
- Unit numbers and plate numbers remain globally unique across both active and
  archived rows.
- An identifier used by an archived bus cannot be assigned to a new bus.
- This preserves historical links to future dispatch, maintenance, incident,
  status, and audit records.

### 6. Created the first real Flyway migration

Created:

```text
src/main/resources/db/migration/V1__create_fleet_units.sql
```

The migration creates `arc.fleet_units` with:

- an identity primary key;
- a permanent unique unit number;
- a permanent unique plate number;
- a bus-only vehicle type;
- positive passenger capacity;
- an `INACTIVE` default status;
- Java-managed creation and update timestamps;
- an optional archival timestamp;
- a JPA optimistic-lock version.

The migration also defines named constraints for:

- primary-key enforcement;
- unit-number uniqueness;
- plate-number uniqueness;
- bus-only vehicle type;
- positive capacity;
- accepted operational-status values.

Detailed comments were added directly to the SQL migration so the file can serve
as both executable schema code and a future SQL study reference.

The migration was applied by running:

```cmd
mvnw.cmd clean test
```

Verified result:

```text
Tests run: 1
Failures: 0
Errors: 0
BUILD SUCCESS
```

### 7. Added Spring Modulith dependency management

Added the Spring Modulith version property:

```xml
<spring-modulith.version>2.1.0</spring-modulith.version>
```

Imported the Spring Modulith BOM under Maven `dependencyManagement`.

Added:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
```

Added test-only architecture support:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

The duplicate BOM declaration was removed from the normal dependency list. The
BOM remains only under `dependencyManagement`, where it supplies compatible
versions to the actual Modulith dependencies.

The dependency tree was inspected with:

```cmd
mvnw.cmd dependency:tree -Dincludes=org.springframework.modulith
```

Verified resolution:

- `spring-modulith-starter-core:2.1.0` with compile scope;
- `spring-modulith-starter-test:2.1.0` with test scope;
- all resolved Spring Modulith artifacts use version 2.1.0;
- no mixed Modulith versions were reported.

### 8. Reorganized the Java source tree by business module

The original source tree contained:

```text
com.transit.arctransit
├── ArcTransitSystemApplication.java
└── ui
    └── DashboardView.java
```

A root-level `ui` package would be detected as a separate application module.
The source tree was therefore reorganized around Arc Transit business
capabilities.

The dashboard was moved from:

```text
com.transit.arctransit.ui.DashboardView
```

to:

```text
com.transit.arctransit.analytics.ui.DashboardView
```

This makes the Analytics module the owner of the dashboard interface instead of
creating an unrelated top-level UI module.

Created the following direct application-module packages:

```text
com.transit.arctransit.analytics
com.transit.arctransit.audit
com.transit.arctransit.auth
com.transit.arctransit.dispatch
com.transit.arctransit.driver
com.transit.arctransit.fleet
com.transit.arctransit.incident
com.transit.arctransit.maintenance
com.transit.arctransit.route
```

Each module received a `package-info.java` file containing:

- a human-readable module description;
- `@ApplicationModule`;
- a display name matching the module responsibility.

No speculative `allowedDependencies` rules were added yet. Actual dependencies
will be declared only after real application-service contracts and
cross-module calls exist.

### 9. Added automated Spring Modulith verification

Created:

```text
src/test/java/com/transit/arctransit/ModularityTests.java
```

The test builds the Modulith application model from the main Spring Boot class:

```java
ApplicationModules modules =
        ApplicationModules.of(ArcTransitSystemApplication.class);
```

It then runs:

```java
modules.verify();
```

The verification checks the current package arrangement for architecture
violations such as:

- cyclic dependencies between application modules;
- illegal access to another module's internal packages;
- invalid dependency rules when those rules are later declared.

The focused test was executed with:

```cmd
mvnw.cmd -Dtest=ModularityTests test
```

Spring Modulith detected exactly these nine modules:

1. Analytics
2. Audit
3. Authentication
4. Dispatch
5. Driver
6. Fleet
7. Incident
8. Maintenance
9. Route

Verified result:

```text
Tests run: 1
Failures: 0
Errors: 0
BUILD SUCCESS
```

The complete Maven test suite was then run again and also completed
successfully.

---

## Friday — July 24, 2026

### 1. Continued the authentication database migration review

- Reviewed the current Fleet and authentication Flyway migrations against the
  manually rebuilt repository.
- Continued revising the SQL comments so they describe actual PostgreSQL and JPA
  behavior rather than generated assumptions.
- Corrected the `app_users` identity-column declaration to use valid PostgreSQL
  identity syntax.
- Retained PostgreSQL as the final data-integrity boundary for identifiers,
  account states, uniqueness, relationships, and role assignments.
- Deferred the final SQL completion decision until every V1 and V2 section has
  been reviewed individually.

### 2. Removed the unsafe permanent development-administrator migration

- Removed `V3__seed_dev_admin.sql` from the current migration sequence.
- Confirmed that a known development password or placeholder password hash
  should not become part of permanent Flyway migration history.
- Retained the fixed `SYSTEM_ADMIN` and `OPERATIONS_STAFF` role definitions as
  database reference data.
- Separated required role data from development-only account creation.

### 3. Replayed the migrations against a clean PostgreSQL database

- Removed the disposable local PostgreSQL volume so Flyway could rebuild the
  database from an empty state.
- Started the PostgreSQL service again through Docker Compose.
- Confirmed that the PostgreSQL container reached its healthy state.
- Used the reset only because the current development database contained no
  permanent data.

Representative commands:

```cmd
docker compose down -v
docker compose up -d postgres
docker compose ps
```

### 4. Modernized AppUser Entity and Auth Settings (Saturday — July 25, 2026)

- Fixed a bug where `AccountStatus` was an unused enum and the database state was stored as a raw String in `AppUser.java`.
- **Why this was needed**: Strings lack type-safety. Using `@Enumerated(EnumType.STRING)` allows Hibernate to safely map Java Enums to the database VARCHAR column. If a developer types "active" instead of "ACTIVE", it won't compile, preventing a runtime error. 
- **Documentation Verification**: [Jakarta Persistence 3.2 - Enumerated Annotation](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a14935) (Ctrl+F: `EnumType.STRING`).
- Updated `isActive()` in `AppUser.java` to use `==` instead of `.equals()`. Because Java Enums are singletons, `==` is null-safe, faster, and considered best practice.
- Fixed `AppUserDetailsService.java` to properly compare the enum instead of comparing a literal String `"LOCKED"` to an `AccountStatus` object, which would have compiled but always incorrectly evaluated to true.
- **Documentation Verification**: [Spring Security UserDetails](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details.html) (Ctrl+F: `isEnabled`). We separated `enabled` and `accountNonLocked` checks so Spring Security throws the exact exception (`DisabledException` vs `LockedException`) based on the correct default pre-authentication check order.
- Added `spring.profiles.active=dev` to `application.properties`.
- **Why this was needed**: The `DevAdminSeeder` is annotated with `@Profile("dev")`. Because the profile wasn't active, the admin account was never seeded into PostgreSQL on startup, meaning no one could log in.
- **Documentation Verification**: [Spring Boot Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html) (Ctrl+F: `spring.profiles.active`).

### 5. Exception Foundation and User Administration Service (Phase 2)

- Created a shared `com.transit.arctransit.common.exception` package to house domain-neutral standard exceptions (`ResourceNotFoundException`, `BusinessConflictException`, `CommandValidationException`).
- **Why this was needed**: Throwing generic `RuntimeException`s makes it impossible to globally handle different HTTP status codes cleanly. These exceptions extend `RuntimeException` and use `@ResponseStatus`, allowing Spring MVC to translate them into 404, 409, and 400 HTTP errors automatically, requiring zero boilerplate `@ExceptionHandler` code.
- **Documentation Verification**: [Spring Boot Error Handling](https://docs.spring.io/spring-boot/reference/web/spring-mvc.html#web.servlet.spring-mvc.error-handling) (Ctrl+F: `@ResponseStatus`).
- Created the `UserAdministrationService` public API contract using **Java 21 Records** (`UserView`, `CurrentUserView`, `CreateUserCommand`, etc.) located in the `auth` root package.
- **Why this was needed**: Java 21 `record`s completely eliminate the need for mutable DTOs, Lombok `@Data`, getters, and setters. They are inherently thread-safe and immutable. The root package was chosen so they form the public API of the Authentication module as enforced by Spring Modulith.
- **Documentation Verification**: [Java 21 Records](https://docs.oracle.com/en/java/javase/21/language/records.html) (Ctrl+F: `record class`). 
- Implemented `AppUserAdministrationService` inside `com.transit.arctransit.auth.application`, using implicit constructor injection (no `@Autowired`).
- **Why this was needed**: Spring Modulith treats subpackages like `application` as internal. Other modules (like `fleet`) cannot access this class directly, forcing them to depend solely on the public interface `UserAdministrationService`, thereby decoupling the system.
- Enforced security rules using `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` directly on the service methods, ensuring authorization checks occur regardless of whether the Vaadin UI or a future REST API calls the service.
- **Documentation Verification**: [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html) (Ctrl+F: `@PreAuthorize`).

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

- Audit persistence and business-action recording;
- Fleet Java persistence and application workflow;
- Fleet Vaadin management interface;
- Fleet tests.
