<img src="https://readme-typing-svg.herokuapp.com?font=Anaheim&size=32&duration=3000&pause=2000&color=1F51FF&width=1000&lines=Arc+Transit+System;Transit+Operations+Command+and+Fleet+Intelligence+Platform" alt="Typing SVG" />

A bus transit management system built as a university project. Handles fleet registration, driver management, route planning, dispatching, incident reporting, maintenance tracking, and operational dashboards.

## Tech Stack

- **Java 21** with **Spring Boot 4.1.0**
- **Vaadin 25.2.3** (web UI framework — no separate frontend needed)
- **Spring Security** with BCrypt password hashing
- **Spring Data JPA** + **Hibernate** (database access)
- **PostgreSQL 17** (runs in Docker)
- **Flyway** (database migrations)
- **Spring Modulith 2.1.0** (enforces module boundaries)

## Project Modules

The codebase is organized into Spring Modulith modules under `com.transit.arctransit`:

| Module | What it owns |
|--------|-------------|
| `analytics` | Dashboard and operational summaries |
| `auth` | Staff accounts, login, roles, Spring Security config |
| `fleet` | Bus registration and operational status |
| `driver` | Driver identity, license, and availability |
| `route` | Fixed routes, stops, and schedule templates |
| `dispatch` | Vehicle/driver/route assignments |
| `incident` | Operational incident reports |
| `maintenance` | Scheduled and corrective maintenance |
| `audit` | Append-only records of business changes |

## Prerequisites

- **Java 21** (JDK, not just JRE)
- **Docker** and **Docker Compose**
- **Maven** (or use the included `mvnw` wrapper)

## Getting Started

### 1. Clone the repo

```bash
git clone https://github.com/chuckzxxmello/arc-transit.git
cd arc-transit
```

### 2. Create your `.env` file

Create a `.env` file in the project root (this is gitignored):

```env
POSTGRES_DB=arc_transit
POSTGRES_USER=arc_application
POSTGRES_PASSWORD=your_password_here
```

### 3. Start the database

```bash
docker compose up -d
```

This spins up a PostgreSQL 17 container on port **5433**.

### 4. Set the password environment variable

Before running the app, make sure `POSTGRES_PASSWORD` is set in your environment. In PowerShell:

```powershell
$env:POSTGRES_PASSWORD="your_password_here"
```

Or configure it in your IDE's run configuration under environment variables.

### 5. Run the application

From your IDE, run `ArcTransitSystemApplication.java`. Or from the terminal:

```bash
./mvnw spring-boot:run
```

The app starts at **http://localhost:8080**.

### 6. Log in

A dev admin account is created on first startup:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin` |

## Database Migrations

Flyway runs automatically on startup. Migration files are in `src/main/resources/db/migration/`:

| Migration | What it does |
|-----------|-------------|
| `V1` | Creates the `fleet_units` table |
| `V2` | Creates `roles`, `app_users`, and `user_roles` tables, seeds the two role definitions |
| `V3` | Seeds a dev admin account (password is set at runtime by the app) |

### Resetting the database

If migrations get stuck or you want a fresh start:

```bash
docker compose down -v
docker compose up -d
```

This wipes the database volume and starts clean.

## Running Tests

```bash
./mvnw test
```

Make sure Docker is running (the `contextLoads` test needs the database).

To run only the modularity structure test:

```bash
./mvnw -Dtest=ModularityTests test
```

## Detailed Logging of Documentation References and File Paths

### 1. Database SQL Migration Scripts (`src/main/resources/db/migration/`)

| File Path | Description & Purpose |
|-----------|----------------------|
| `V1__create_fleet.sql` | Schema migration for `arc.fleet_units` table with operational status enums and soft-delete timestamp columns. |
| `V2__create_users.sql` | Schema migration for `arc.roles`, `arc.app_users`, and `arc.user_roles` security mapping tables. |
| `V3__create_drivers.sql` | Schema migration for `arc.drivers` table with named constraints and license expiry tracking. |
| `V4__create_routes_and_stops.sql` | Schema migration for parent `arc.routes` and child `arc.route_stops` with cascade deletion rules. |
| `V5__create_dispatch_assignments.sql` | Schema migration for `arc.dispatch_assignments` with partial unique indexes preventing double-booking. |
| `V6__create_incident_reports.sql` | Schema migration for operational incident reports (`arc.incident_reports`). |
| `V7__add_dispatch_archive.sql` | Schema update adding `archived_at` timestamp column to `arc.dispatch_assignments` for soft deletion. |
| `V8__create_audit_logs.sql` | Schema migration for append-only audit ledger (`arc.audit_logs`) capturing business actions and login events. |

### 2. Core Java Application & Security Components (`src/main/java/com/transit/arctransit/`)

| File Path | Component & Architectural Role |
|-----------|--------------------------------|
| `auth/security/SecurityConfig.java` | Spring Security configuration enforcing form-based login, session management, and HTTP Security Headers (`frameOptions.sameOrigin()`, `contentTypeOptions()`). |
| `auth/security/AuthenticationEventListener.java` | Event listener capturing Spring Security login success (`USER_LOGIN_SUCCESS`) and failure (`USER_LOGIN_FAILED`) events, writing audit logs to `arc.audit_logs`. |
| `auth/CreateUserCommand.java` | Command record annotated with Jakarta `@Pattern` regular expression enforcing strong password complexity (min 8 chars, uppercase, lowercase, number, special char). |
| `auth/UserAdministrationService.java` | Service interface defining user staff account administration, role replacement, and account deletion. |
| `auth/application/AppUserAdministrationService.java` | Implementation of user administration with `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` authorization checks. |
| `auth/ui/UserAdministrationView.java` | Vaadin administration view for creating staff accounts, deleting accounts with `ConfirmDialog`, and promoting roles to `SYSTEM_ADMIN`. |
| `dispatch/DispatchResult.java` | Java 21 `sealed interface` with `Success` and `Failed` record implementations for dispatch operations. |
| `dispatch/DispatchService.java` | Service interface managing dispatch assignments, status lifecycle state machine (`SCHEDULED -> IN_PROGRESS -> COMPLETED`), and archiving. |
| `analytics/ui/ArchiveView.java` | Vaadin archive view with 4 tabs (Buses, Drivers, Routes, Dispatches), Unarchive action buttons, and `@RolesAllowed("SYSTEM_ADMIN")` authorization. |
| `route/ui/RouteManagementView.java` | Route management view featuring inline dynamic stop list editing with trash icon `removeBtn` stop deletion. |
| `common/ui/MainLayout.java` | Navigation drawer checking user authorities and restricting view access (`OPERATIONS_STAFF` sees only Dashboard and Dispatch). |
| `common/ui/CustomAccessDeniedView.java` | Custom HTTP 403 error view implementing `HasErrorParameter<AccessDeniedException>` with a primary "Go Back to Dashboard" button. |
| `common/ui/CustomNotFoundView.java` | Custom HTTP 404 error view implementing `HasErrorParameter<NotFoundException>` with a primary "Go Back to Dashboard" button. |

### 3. Application & Infrastructure Configurations

| File Path | Description |
|-----------|-------------|
| `src/main/resources/application.properties` | Spring Boot configuration enabling Gzip HTTP response compression (`server.compression.enabled=true`), HikariCP connection pool limits, and Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`). |
| `.env` | Environment configuration file defining database connection credentials (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`). |
| `pom.xml` | Maven build file defining dependencies for Spring Boot 3.2.5, Vaadin 25.2.3, Spring Modulith 2.1.0, and Jakarta Validation. |

### 4. Documentation References & Technical Standards

- **OWASP Top 10 Web Application Security Risks (2021):** [https://owasp.org/www-project-top-ten/](https://owasp.org/www-project-top-ten/)
- **OWASP A01:2021 — Broken Access Control:** [https://owasp.org/Top10/A01_2021-Broken_Access_Control/](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- **OWASP A05:2021 — Security Misconfiguration:** [https://owasp.org/Top10/A05_2021-Security_Misconfiguration/](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)
- **Spring Security Architecture & Headers:** [https://docs.spring.io/spring-security/reference/servlet/architecture.html](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- **Vaadin 25 Security & `@RolesAllowed` Guide:** [https://vaadin.com/docs/latest/security](https://vaadin.com/docs/latest/security)
- **Oracle Java 21 Sealed Classes & Interfaces:** [https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html](https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html)
- **Spring Modulith 2.1.0 Reference Documentation:** [https://docs.spring.io/spring-modulith/reference/](https://docs.spring.io/spring-modulith/reference/)

