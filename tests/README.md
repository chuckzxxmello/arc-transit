# Arc Transit System — Test Documentation

## Test Categories

| # | Document | Scope |
|---|----------|-------|
| 1 | [UI_Interface_Test.md](UI_Interface_Test.md) | Login, navigation, views, responsive layout, error pages |
| 2 | [Feature_Functionality_Test.md](Feature_Functionality_Test.md) | CRUD operations, state machines, validations, security |
| 3 | [Backend_Performance_Test.md](Backend_Performance_Test.md) | Response times, throughput, database queries, connection pool |

## How to Run

1. Start PostgreSQL: `docker compose up -d postgres`
2. Start the app: `./mvnw spring-boot:run`
3. Run automated tests: `./mvnw clean test`
4. Fill in each `.md` file with measured results during manual QA.
