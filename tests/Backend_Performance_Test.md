> Historical manual record. Earlier statuses and expectations have not been reverified for the redeveloped application. Use tests/README.md and CODEX_PROGRESS.md for current procedures and evidence.

# Backend Speed & Performance Test Results

## 1. Application Startup

| # | Metric | How to Measure | Target | Actual | Status |
|---|--------|---------------|--------|--------|--------|
| PT-01 | Cold start time | Terminal output: "Started ArcTransitSystemApplication in X seconds" | < 30s | ____ s |  |
| PT-02 | Flyway migration time | Terminal output: migration log timestamps | < 5s | ____ s |  |
| PT-03 | Startup diagnostic | All modules verified message in logs | All 5 counters print | |  |

## 2. Page Load Time

| # | Page | URL | Target | Actual | Status |
|---|------|-----|--------|--------|--------|
| PT-04 | Login | `/login` | < 2s | ____ ms |  |
| PT-05 | Dashboard | `/` | < 3s | ____ ms |  |
| PT-06 | Fleet Management | `/fleet` | < 2s | ____ ms |  |
| PT-07 | Driver Management | `/drivers` | < 2s | ____ ms |  |
| PT-08 | Route Schedule | `/routes` | < 2s | ____ ms |  |
| PT-09 | Dispatch & Assignment | `/dispatch` | < 2s | ____ ms |  |
| PT-10 | Exception Queue | `/exceptions` | < 2s | ____ ms |  |
| PT-11 | User Administration | `/admin/users` | < 2s | ____ ms |  |
| PT-12 | Archived Data | `/archives` | < 2s | ____ ms |  |

## 3. CRUD Response Time

| # | Operation | Module | Target | Actual | Status |
|---|-----------|--------|--------|--------|--------|
| PT-13 | Create fleet unit | Fleet | < 500ms | ____ ms |  |
| PT-14 | Search/list fleet units (10 records) | Fleet | < 300ms | ____ ms |  |
| PT-15 | Update fleet unit | Fleet | < 500ms | ____ ms |        |
| PT-16 | Archive fleet unit | Fleet | < 500ms | ____ ms |  |
| PT-17 | Create driver | Driver | < 500ms | ____ ms |  |
| PT-18 | Create route (3 stops) | Route | < 500ms | ____ ms |  |
| PT-19 | Create dispatch assignment | Dispatch | < 800ms | ____ ms |  |
| PT-20 | Start trip state transition | Dispatch | < 500ms | ____ ms |  |
| PT-21 | Run reconciliation engine | Reconciliation | < 2s | ____ ms |  |

## 4. Database Performance

| # | Metric | How to Measure | Target | Actual | Status |
|---|--------|---------------|--------|--------|--------|
| PT-22 | Connection pool size | `application.properties` → `hikari.maximum-pool-size` | 10 | |  |
| PT-23 | Min idle connections | `application.properties` → `hikari.minimum-idle` | 5 | |  |
| PT-24 | Connection timeout | `application.properties` → `hikari.connection-timeout` | 20000ms | |  |
| PT-25 | Idle timeout | `application.properties` → `hikari.idle-timeout` | 300000ms | |  |
| PT-26 | Total migrations applied | Count files in `db/migration/` | 8 | |  |
| PT-27 | Schema validation mode | `spring.jpa.hibernate.ddl-auto` | validate | |  |

## 5. Build & Test Suite Performance

| # | Metric | Command | Target | Actual | Status |
|---|--------|---------|--------|--------|--------|
| PT-28 | Full build time | `./mvnw clean compile` | < 45s | ____ s |  |
| PT-29 | Full test suite time | `./mvnw clean test` | < 120s | ____ s |  |
| PT-30 | Total test count | `./mvnw test` output | >= 39 | ____ |  |
| PT-31 | Test failures | `./mvnw test` output | 0 | ____ |  |
| PT-32 | Modularity test time | `./mvnw -Dtest=ModularityTests test` | < 30s | ____ s |  |
| PT-33 | Context load test time | `./mvnw -Dtest=ArcTransitSystemApplicationTests test` | < 45s | ____ s |        |

## 6. Compression & Optimization

| # | Metric | How to Measure | Target | Actual | Status |
|---|--------|---------------|--------|--------|--------|
| PT-34 | Gzip enabled | `application.properties` → `server.compression.enabled` | true | |  |
| PT-35 | Virtual threads enabled | `application.properties` → `spring.threads.virtual.enabled` | true | |  |
| PT-36 | Compressed response | DevTools → Network → check `Content-Encoding: gzip` on JS/CSS | gzip header present | |  |
| PT-37 | Transfer size vs actual | DevTools → Network → compare transferred vs resource size for main bundle | Transferred < 50% of actual | ____ KB / ____ KB |  |

**Summary**

| Category | Tests | Passed | Failed |
|----------|-------|--------|--------|
| Application Startup | 3 | | |
| Page Load Times | 9 | | |
| CRUD Response Times | 9 | | |
| Database Performance | 6 | | |
| Build & Test Suite | 6 | | |
| Compression & Optimization | 4 | | |
| Code Metrics | 5 | | |
| **TOTAL** | **42** | | |
