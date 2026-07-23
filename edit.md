---
title: Arc Transit System Manual Rebuild Log
project: Arc Transit System
status_date: 2026-07-23
---

# Arc Transit System — Manual Rebuild Implementation Log

## Manual Rebuild Status — 2026-07-23

A new Arc Transit System repository is being rebuilt manually. The previous
generated implementation remains available only as a read-only architectural
reference.

The purpose of this rebuild is to reproduce the system through understandable,
reviewable, and verified development steps. A feature is not treated as complete
just because it existed in the previous generated codebase. It must be recreated,
tested, and recorded in the new repository.

## Documentation Rule

Only behavior verified in the new repository may be marked **Completed**.

Evidence may include:

- a successful Maven build or test result;
- a successful application startup;
- a verified PostgreSQL connection;
- a successfully applied Flyway migration;
- a Spring Modulith verification result;
- a browser-visible Vaadin route;
- a reviewed Git commit containing the implementation.

The previous generated repository must not be used as proof that a manual rebuild
task is complete.

---

# Verified Project Foundation

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

---

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

---

# Current Verified Status

## Week 5 — Application Foundation

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

## Current Limitations

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

---

# Next Implementation Target — Week 6

Week 6 focuses on **Security, Audit, and Fleet**.

The planned order is:

1. Create the authentication tables:
   - `roles`
   - `app_users`
   - `user_roles`
2. Seed the fixed role definitions:
   - `SYSTEM_ADMIN`
   - `OPERATIONS_STAFF`
3. Add Spring Security and Vaadin security integration.
4. Implement BCrypt password encoding.
5. Create a development-only administrator account without committing a
   plaintext production password.
6. Implement the Vaadin login view.
7. Protect application navigation.
8. Verify login, logout, disabled-account rejection, and role restrictions.
9. Create the audit foundation.
10. Implement the first complete Fleet workflow from Vaadin to PostgreSQL.

---

# Implementation Plan Note

> **Current implementation note — 2026-07-23:**  
> This guide originally described the previous generated implementation.
> The project is now being rebuilt manually in a new repository. Treat
> unverified sections as reference material until they are reproduced,
> tested, and recorded in the new repository.
