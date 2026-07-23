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
