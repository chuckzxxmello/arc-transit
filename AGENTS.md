# Arc Transit Codex Instructions

This repository contains an existing Arc Transit implementation being redeveloped into the latest Arc Transit specification.

## Core behavior

- Read and understand existing code before changing it.
- Preserve working implementation when it remains compatible with current requirements.
- Do not perform a clean-slate rewrite.
- Do not destroy existing developer changes.
- Prefer the smallest technically correct change.
- Follow existing package, naming, formatting, and coding conventions.
- Preserve the existing UI/UX visual identity unless functionality requires a change.
- Do not mass-format or mass-rename unrelated code.
- Do not introduce unnecessary abstractions or dependencies.

## Current architecture

Arc Transit is a Java modular monolith using the existing project stack.

Expected architecture:

Browser
-> Vaadin Flow
-> Spring Boot application services
-> Spring Modulith business modules
-> Spring Data JPA / selected JDBC
-> PostgreSQL

Flyway owns schema migrations.

Do not convert the application to microservices.

Do not introduce Kafka, Redis, RabbitMQ, Kubernetes, another frontend framework, or unrelated infrastructure.

## Important domain distinctions

Keep these separate:

- fleet identity != vehicle availability != dispatch assignment
- driver identity != availability != qualification != trip assignment
- route identity != route version != schedule != trip instance
- current state != historical event
- operational report != verified physical truth
- validation error != reconciliation exception
- audit log != business-event history

## System scope

Arc Transit is a staff-facing transit operations coordination and operational-state reconciliation application.

It is not:

- a live GPS tracker
- an AVL platform
- a telemetry platform
- a passenger application
- an autonomous dispatch system
- a route optimization engine
- a predictive maintenance system
- an AI operations controller
- a digital twin
- a fare system
- a blockchain system

Human-entered information is an important source of operational state.

Never present manually entered records as proven physical truth.

## Database safety

- Inspect existing entities and Flyway migrations before changing schema.
- Do not casually edit already-applied migrations.
- Prefer a new migration for established schema changes.
- Do not drop tables, columns, data, or Flyway history without clear justification.
- Do not reset the database merely because migration is easier that way.
- Preserve historical records where possible.
- Keep important state changes transactional.

## Git safety

Never run destructive commands against unknown developer work, including:

- git reset --hard
- git clean -fd
- git checkout -- .
- broad git restore commands

Do not commit or push unless explicitly requested.

Always inspect git status and git diff before substantial work.

## UI

Preserve the existing Arc Transit UI design.

Do not redesign the application into a generic admin dashboard.

Modify UI only when required for:

- functionality
- validation
- correct information
- workflow completion
- security
- justified feature removal

## Map functionality

Do not automatically remove or keep map functionality.

Static route presentation is acceptable.

Live GPS/AVL tracking, fake live vehicle positions, automatic telemetry, and automatic ETA are outside the current scope.

Inspect the implementation and decide based on actual behavior.

## Code style

Follow the existing author's code style.

Do not add obvious or robotic comments.

Do not use AI-style comments such as:

- "Enhanced validation"
- "Process data efficiently"
- "Ensure seamless operation"
- "Robust implementation"

Only comment non-obvious domain rules, security reasons, database behavior, or necessary workarounds.

Do not use emojis.

Do not use em dash characters.

Use plain professional technical English.

## Verification

Do not claim something works without verification.

Distinguish:

VERIFIED
IMPLEMENTED BUT NOT EXECUTED
NOT VERIFIED

Run relevant builds/tests after changes.

Prefer meaningful business tests over superficial test count.