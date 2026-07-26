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

- Audit persistence and business-action recording;
- Fleet Java persistence and application workflow;
- Fleet Vaadin management interface;
- Fleet tests.
