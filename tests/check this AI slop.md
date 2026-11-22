# Arc Transit Verification

Run the current suite with a separate configured PostgreSQL test database:

    .\mvnw.cmd test

On Windows with PostgreSQL 17 installed, use an isolated cluster without modifying the application database:

    .\scripts\verify-postgres.ps1 -MavenArguments @('test')
    .\scripts\verify-postgres.ps1 -MavenArguments @('verify', '-Dvaadin.productionMode=true') -SmokeTest

The script starts a loopback-only cluster, supplies temporary credentials and ARC_TEST_POSTGRES=true, runs Maven, and stops the cluster. Logs remain under .verification. Run clean separately before the script. Smoke tests launch a copied JAR under .verification to avoid locking the JAR in target. Stop any application launched directly from target before running clean. The Windows Maven wrapper selects JDK 21; ARC_JAVA_HOME can select its installation explicitly.

| Automated evidence | Coverage |
| --- | --- |
| Existing and extended unit tests | Fleet/driver identity and availability, maintenance lifecycle and holds, dispatch rules and intervals, route versions and schedules |
| Service proxy authorization tests | Direct staff/admin invocation, account validation, protected user changes, denial audit and privacy |
| Password policy and Vaadin form tests | Standard punctuation including #, shared creation/reset validation, invalid field states, live strength labels and prevention of invalid submissions |
| Account archive integration tests | Committed archiving, retained identity/roles, disabled authentication, separated active/archive search, blank filters, case-insensitive matches and paging against PostgreSQL |
| Account editing integration/UI tests | Prefilled editing, atomic profile/role changes, stable IDs/passwords, username reservation and current-login behavior, stale-edit protection, last-administrator guard and self-edit sign-out |
| Fleet deletion integration tests | Automatic history/audit records do not block deletion, real operational links do block it, history remains immutable, and audit failure rolls deletion back |
| Reconciliation tests | Known contradictions, ordinary controls, repeat-scan deduplication, review notes and report review |
| PostgreSQL workflow tests | Current/history atomicity and rollback, real concurrent assignment attempts, route history, report provenance, protected history and configured-trip lifecycle concurrency |
| Migration upgrade test | V11 representative records preserved through forward migrations, incomplete legacy intervals retained and recoverable |
| ModularityTests | Spring Modulith boundaries and dependency cycles |
| CompletedWorkArchiveIntegrationTest | Committed completion/closure archiving, retained notes/history, archive permissions, availability rules and rollback when archive audit recording fails |
| CompletedWorkMigrationIntegrationTest | V24-to-V25 classification of completed maintenance and closed incidents without losing pending/cancelled/resolved records or notes |
| OperationalWindowsTest report/grid checks | Human record selection submits the actual ID, switching record type clears the selection, invalid selection cannot submit, archive details remain draggable, and attached grids expose resizable columns without flex compression |
| OperationalReportArchiveIntegrationTest | Archive/restore filters and dashboard counts, review rules, immutable history after deletion, audit snapshots, incident/exception blockers, permissions, audit-failure rollback and concurrent reference insertion versus deletion |
| OperationalReportArchiveMigrationTest | V25-to-V26 preservation of existing reports, incident links and immutable verification history, including deletion without orphan event insertion |

See ../CODEX_PROGRESS.md for exact commands and actual results.

The individual UI_Interface_Test.md, Feature_Functionality_Test.md and Backend_Performance_Test.md documents are historical manual procedures. Their earlier Pass entries are not verification of the redeveloped application. Some old expectations are superseded: new availability is UNKNOWN; employment is separate from duty availability; maintenance completion does not assert AVAILABLE; route definitions are versioned; overlapping intervals replace whole-day blocking; account removal archives identity.

Current browser/manual acceptance still needs execution. Performance proposal targets require controlled measurements and are not established by the business test suite.

Current input limit and negative form regression procedures: [docs/INPUT_VALIDATION.md](../docs/INPUT_VALIDATION.md). These supersede older password-length and first-100-selector expectations.

For direct PostgreSQL fixture loading, rollback-only constraint probes, and application CRUD verification, follow [docs/POSTGRES_CRUD_TESTING.md](../docs/POSTGRES_CRUD_TESTING.md). The SQL files under tests/sql require a dedicated arc_transit_crud_test database. SqlCrudFixtureIntegrationTest verifies them in an isolated migrated database; SQL acceptance does not replace service authorization or business-rule tests.

For plain browser instructions with realistic sample entries, invalid inputs and the actual buttons to click, use [Arc Transit Practical Web Test Sheet](../docs/Arc_Transit_Practical_Web_Test_Sheet.docx). Its 267 checks across 58 pages start with login, fleet, drivers, users and routes before dispatch, reports, incidents and maintenance. It also covers every archive tab, audit filters, reconciliation and historical scenarios, shared UI behavior, permissions, dashboard, maps, logout and recovery. Copyable boundary inputs, README coverage gaps and tests requiring controlled backend fixtures are identified. Its expected outcomes are manual test instructions, not recorded passing browser results.

The revised sheet also covers capacity 1-80, searchable bus selection in incidents/maintenance, one transactional fleet-state Save, and draggable double-click details in operational modules, all archive tabs and Audit Log. OperationalWindowsTest executes the Java listeners; FleetStateIntegrationTest checks committed validation/atomicity/audit rollback, and FleetCapacityMigrationIntegrationTest verifies legacy capacity preservation during V21-to-V22 migration.

The October 19 revision adds realistic steps for refresh buttons, per-account browser sidebar ordering, driver mobile numbers and archive actions, guarded permanent dispatch/account deletion, and reasoned maintenance unlinking. ArchiveActionsIntegrationTest exercises real PostgreSQL commits, link blockers, retained history/reservations, authorization and rollback when audit insertion fails. OperationalWindowsTest covers the UI actions and server filtering of sidebar order. The forward migration test also retains legacy international contact values through V24. The clarified sidebar allows direct dragging of entire module rows or their dotted handles, with a placeholder and Alt+Up/Down keyboard controls. Ordinary clicks still navigate. Dashboard mapping uses bundled Leaflet, OpenStreetMap tiles and Open-Meteo model weather without a Google key. Browser execution evidence and remaining limitations are recorded in CODEX_PROGRESS.md.

Run `node --test tests/client/*.test.cjs` to check weather failures, rate limiting, malformed responses, zero/missing readings, request cancellation, stale-response isolation and caching with deterministic provider responses. Sidebar control regressions check keyboard saves, delegation of pointer drops to Sortable Layout, Escape cancellation and handle-versus-label clicks. OperationalWindowsTest invokes the add-on's actual reorder handler in both directions and checks server order alongside the authorized-link filter. These tests make no external requests. Use the practical sheet for actual map, navigation and browser persistence checks. The Word sheet's existing handle, keyboard and persistence steps still apply to this add-on.
