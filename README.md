# Arc Transit

Transit Operations Coordination and Operational-State Reconciliation Platform

Arc Transit is a staff-facing web application for maintaining bus, driver, route, dispatch, incident, maintenance, and operational-report records. It helps staff coordinate work, check whether records agree, and review how recorded conditions changed over time. Human-entered information is a primary source of its operational state.

Developed by Chuckz Española. This project overview is an editable documentation draft, revised on 28 October 2026. It follows the latest supplied project proposal and describes the current repository where implementation details are given. Proposed improvements and evaluation targets are identified separately from existing behavior and recorded test results.

## 1. Problem statement and proposed solution

### Problem statement

Transit operations involve several related records that answer different questions. A registered bus may be unavailable for service. An active employee may have an expired license or be unavailable for duty. A route can retain its identity while its stops or service definition change. A planned assignment can later be cancelled, while a report about that assignment may arrive after the event it describes.

When these differences are represented by one status field or by records without clear links and dates, staff can lose the context needed to understand an operational decision. They may see that a bus is active without knowing that maintenance prevents its assignment, or read an earlier trip using a route definition introduced afterward. Conflicting records also need a review process rather than an unexplained correction.

Philippine transport reporting provides a specific example of the distinction between registration and operating availability. In September 2025, the Department of Transportation directed the LTFRB to determine whether 3,972 franchised public utility vehicles on Commonwealth Avenue routes were actually operational. This supports separating administrative identity from operating availability; it does not establish that every operator has inaccurate records. [Philippine Information Agency, 25 September 2025](https://pia.gov.ph/news/dotr-chief-wants-more-puvs-on-commonwealth-avenue/).

Digital dispatch itself is already established. The Philippine Information Agency reported the MMDA Bus Management and Dispatch System's use on the EDSA Busway in June 2023. Arc Transit therefore makes no claim that digitizing dispatch is a new invention or that this prototype replaces an existing transport-control platform. [Philippine Information Agency, 19 June 2023](https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway).

The project addresses a narrower software problem: maintaining an understandable, historically traceable operational record when continuous vehicle feeds are not assumed. Whether the proposed workflows match an operator's actual procedures still requires operator review.

### Proposed solution

Arc Transit brings the related records into one application and applies explicit rules when staff change them. Its main design principles are:

- Separate vehicle identity, recorded availability, and trip assignment.
- Separate driver identity, employment status, qualification, availability, and assignment.
- Keep route identity, effective-dated route definitions, reusable schedules, and individual dated assignments distinct.
- Preserve important business changes alongside current state.
- Record where operational information came from, who entered it, when it was recorded, and when the reported condition applied.
- Block known invalid actions before saving and create review cases for contradictions found afterward.
- Keep business history separate from the audit record of protected application actions.

For example, a staff member records a radio report that a bus has a flat tire. The report retains its source and effective time and begins as unverified. It does not automatically change the bus's availability. Authorized personnel review the information and separately record the necessary incident, maintenance, availability, or dispatch action. Reconciliation can then identify defined inconsistencies among those records.

Reconciliation means checking existing application records against explicit consistency rules. A case identifies the rule and affected records for human review. It does not determine which report describes physical reality or silently resolve an uncertain operational decision.

## 2. Project objectives and success criteria

### Project objectives

The main objective is to develop and evaluate a transit operations prototype that maintains a coherent and traceable operational record using primarily human-entered information.

The project has eight specific objectives:

1. Maintain separate vehicle and driver identities, availability records, qualifications, and assignments so that one status does not represent several unrelated conditions.
2. Preserve effective-dated route definitions and the route version used by each assignment.
3. Support controlled dispatch, incident, maintenance, and reporting workflows with server-side validation.
4. Preserve the source, responsible user, recording time, effective time, and review history of operational reports.
5. Detect the predefined record contradictions covered by the reconciliation rules without changing source records automatically.
6. Restrict actions according to user role and retain useful audit information without copying credentials into audit records.
7. Present operational summaries based on saved information, with clear limits on freshness and verification.
8. Evaluate workflow correctness, historical consistency, authorization, traceability, and performance through repeatable tests.

### Success criteria

The following criteria define the evidence required for evaluation. They are acceptance targets, not a statement that the project has already met them.

| Area | Success criterion | Evidence to retain |
| --- | --- | --- |
| Assignment validation | Every defined valid control case is accepted and every defined invalid case is rejected, with no partial save | Expected and actual results for qualification, availability, route dates, time boundaries, and resource conflicts |
| Historical route context | Later route changes do not change the route version of an existing assignment | Before-and-after references and route-version tests |
| State and business history | Successful changes retain their required history; failed transactions do not leave partial state or history | Transaction, rollback, and history checks |
| Operational reporting | Required source, user, recording time, effective time, affected record, and review details are present | Report and verification-history checks |
| Reconciliation | Each supported seeded contradiction is detected; normal controls are not incorrectly flagged; repeated scans do not duplicate an unresolved case | Expected cases, actual cases, precision, recall, and scan duration |
| Recovery traceability | The required links among a report, affected record, incident, maintenance, and follow-up action can be followed in the tested scenario | Linked record details and scenario results; unsupported links remain documented gaps |
| Authorization | Every action in the agreed role-action matrix permits the correct role and rejects unauthorized access | Service and browser authorization results |
| Audit | Every selected protected action records the required actor, time, action, affected record, and outcome without credential leakage | Audit-completeness checks and inspected samples |
| User workflow | A tester can complete the agreed end-to-end walkthrough using the documented controls | Completed manual test record, errors, fixes, and retest results |
| Build and recovery | The release candidate passes its required build and database tests, starts successfully, and has a demonstrated backup restore | Build logs, startup evidence, migration results, and restore record |

Precision is the proportion of detected cases that match the expected contradictions. Recall is the proportion of known seeded contradictions that the scan detects. Record the counts as well as the percentages, including cases the system misses or flags incorrectly.

The supplied draft also contains these initial performance targets:

| Measure | Planned target | Required test condition |
| --- | --- | --- |
| Dashboard initial load | Under 3 seconds | Authenticated session with a documented dataset of approximately 500 fleet records and related operational data |
| Fleet search and filtering | p95 under 300 milliseconds | Repeatable searches against the same dataset after a defined warm-up |
| Login | p95 under 500 milliseconds | Controlled load of 50 concurrent virtual users |

The p95 value is the response time within which 95 percent of the measured requests finish. Before measuring, document the hardware, software versions, database size, browser or load tool, network conditions, test duration, and measurement boundaries. These draft targets may be revised with a recorded reason before final evaluation. Browser rendering, server execution, and database query time must be reported separately where they measure different parts of an action.

### Current evidence and remaining work

[CODEX_PROGRESS.md](CODEX_PROGRESS.md) records a successful production frontend and JAR build with 253 automated tests passing on 22 October 2026. It also records browser checks for selected features. Those results apply to the source and environment tested at that time; they do not establish that every manual check or performance target has passed.

The same record leaves normal browser text-entry submission unresolved after a registration attempt showed visible values but still reported required fields. That observation needs reproduction and investigation. A complete manual walkthrough, measured performance evaluation, backup/restore exercise, and deployment evaluation remain separate work.

This README revision was checked against source files and configuration. Application builds, database migrations, and browser workflows were not executed as part of the documentation revision.

## 3. Scope, boundaries, and limitations

### Included scope

Arc Transit represents one simulated or test organization, such as a Philippine bus operator or cooperative. It is intended for internal staff using a browser. The current application contains the following areas:

| Area | Included behavior |
| --- | --- |
| Fleet | Bus registration, administrative state, recorded availability, availability history, and controlled archival |
| Drivers | Identity, employment status, license information, separate availability, history, and controlled archival |
| Routes | Stable route identity, effective-dated definitions and stops, reusable departure schedules, and historical references |
| Dispatch | Dated bus-driver-route assignments, planned intervals, eligibility checks, start/completion/cancellation, and business history |
| Operational reports | Staff, phone, and radio reports with affected records, effective times, freshness labels, administrator review, and archival |
| Incidents | Reported issues, affected records, investigation, resolution, closure, history, and supported report/maintenance links |
| Maintenance | Work requests, approval, work in progress, completion/cancellation, history, and effects on assignment eligibility |
| Reconciliation | Deterministic consistency scans, rule-specific exceptions, and recorded human review |
| Dashboard | Descriptive summaries of saved records, with an external road-map and weather reference |
| Archives | Administrator access to retained records, supported restoration, and guarded permanent deletion where provided |
| Accounts and audit | Staff authentication, two application roles, account administration, protected-action records, and administrator audit search |

### Domain boundaries

A bus recorded as administratively active can still be unavailable. A driver recorded as available can still be ineligible because of qualification or an overlapping assignment. A configured departure schedule is a reusable template; it does not mean that a dated trip has already been created.

The current dispatch record represents the dated trip and its resource assignment. There is no separate implemented planning board for unassigned trips. Dispatch currently follows `SCHEDULED -> IN_PROGRESS -> COMPLETED`, with cancellation from scheduled or in-progress state. The longer lifecycle described in the supplied proposal remains a design option, discussed in Section 8.

Route versions apply from their effective start date up to, but excluding, their end date. Planned dispatch intervals similarly include departure and exclude arrival. A trip ending at 10:00 and another beginning at 10:00 can therefore be adjacent without overlapping. Historical assignments retain their route-version references. Migration snapshots preserve the legacy route definition available at migration time; they cannot recover older definitions that were already overwritten.

Immediate validation and reconciliation serve different purposes. Validation rejects an invalid submission. Reconciliation examines saved records and opens a review case when a supported rule identifies a contradiction or aging condition. Dismissing or resolving an exception does not itself rewrite the original operational records.

Business history describes changes such as starting a trip or reviewing a report. Audit history records protected application actions and their outcomes. Append-only protections support traceability, but they do not make the database tamper-proof against infrastructure administrators.

### Exclusions

The present project excludes live GPS or Automatic Vehicle Location (AVL), continuous vehicle telemetry, automatic estimated arrival times, predictive maintenance, autonomous dispatch, route or crew optimization, passenger applications, fares and payments, live passenger-information publishing, multi-tenant operation, and city-wide traffic control. It also excludes AI operations control, machine-learning anomaly detection, digital-twin claims, regulatory enforcement, and blockchain functionality.

The map is an external geographic reference. It does not show live vehicles, verified traffic conditions, or automatic dispatch decisions. Configured route-stop records do not establish that the current dashboard draws each route on the map.

### Limitations and assumptions

The project assumes authorized staff will maintain resource records and enter operational changes. Recording a report as confirmed means an authorized person recorded a review decision; it is not independent proof that the reported physical event occurred. Availability does not certify mechanical roadworthiness, and recorded trip completion does not prove that a bus completed the journey.

No operator interview, live fleet feed, or production operational dataset is established by the supplied project material. Controlled examples and synthetic scenarios can evaluate software behavior, but they cannot demonstrate reduced travel time, improved road safety, lower operating cost, or better passenger service in an actual fleet.

Reconciliation covers only its implemented rules. Freshness labels describe information age, not truth. A report can be current and wrong, or stale and historically accurate. An old report is not automatically deleted or rejected.

The prototype depends on its application server, PostgreSQL, and browser connection. The road-map and weather panels additionally require internet access and external providers. An application or database failure interrupts work; no offline synchronization, high-availability cluster, or automatic failover is provided.

Performance, accessibility, privacy governance, retention periods, backup schedules, and operational readiness still need evaluation in the intended environment. Existing controls are not evidence of complete security or legal compliance. Production use requires decisions about lawful data handling, access, retention, recovery, and external-service licensing.

## 4. Users

The application currently has two roles. Drivers and passengers do not have a dedicated application role or client. A driver record is an operational record, not a staff login account.

| User | Purpose | Current browser access |
| --- | --- | --- |
| System administrator (`SYSTEM_ADMIN`) | Manage accounts and protected records, oversee operations, review reports and exceptions, and inspect history | All operational screens, Incident Management, Maintenance Management, Exception Queue, Archived Data, User Administration, and Audit Log |
| Operations staff (`OPERATIONS_STAFF`) | Maintain routine resource information, create and update assignments, and submit received information | Dashboard Overview, Dispatch & Assignment, Fleet Availability, Driver Management, Route Schedule, and Operational Reports |

Both roles can use the ordinary resource and dispatch workflows exposed on their screens. Administrator-only actions include resource archival, report review and archival, account management, and protected archive actions. Service authorization also applies to individual actions; access to a screen is not permission to perform every possible operation.

The latest proposal describes a broader operations-staff role for incidents and maintenance. The current browser restricts those modules to administrators. Any change to that policy needs an agreed permission matrix and corresponding implementation and tests. This overview documents the existing restriction.

Other project stakeholders include the developer and maintainer, people reviewing the proposed operator workflow, and evaluators who inspect software evidence. These responsibilities do not create additional system permissions. An organization's maintenance personnel or supervisors would need an explicitly approved access model before receiving separate roles.

## 5. Feasibility and technical constraints

### Architecture and implementation approach

Arc Transit uses one Java application with internal business modules, commonly called a modular monolith. It is built and deployed as one unit, while application services separate responsibilities and control validation, authorization, and transactions.

The normal application flow is:

```text
Browser
  -> Vaadin Flow
  -> Spring Boot application services and Spring Modulith modules
  -> Spring Data JPA / selected JDBC
  -> PostgreSQL
```

The browser interface and application services share the same application. The staff interface does not require a separate public REST API. Internal module calls use published Java interfaces rather than internal HTTP services or direct access to another module's repositories.

| Technology | Repository baseline and purpose |
| --- | --- |
| Java | JDK 21 for compilation and runtime |
| Spring Boot | Version 4.1.0 declared in `pom.xml`; application runtime, configuration, and transactions |
| Vaadin Flow | Vaadin 25.2.3 declared in `pom.xml`; Java-based browser interface and existing application design |
| Spring Modulith | Version 2.1.0 declared in `pom.xml`; module organization and verification |
| Spring Security | Authentication, BCrypt password hashing, protected navigation, service authorization, and session controls |
| Spring Data JPA / JDBC | Relational persistence and selected explicit queries |
| PostgreSQL | Version 17 in the Compose configuration; relational data, constraints, and transactions |
| Flyway | Versioned schema migrations; Hibernate validates mappings rather than creating the schema |
| Maven Wrapper | Project build, automated tests, and packaging |
| Docker Compose | Local PostgreSQL and a separate application-plus-database deployment configuration |
| Leaflet and external providers | Locally bundled Leaflet 1.9.4, OpenStreetMap road tiles, and Open-Meteo weather |
| Sortable Layout | Vaadin add-on 3.0.1 for sidebar ordering |

Declared dependency versions describe the checked repository configuration. They are not a recommendation to upgrade or a claim about the latest vendor release.

The business modules are `auth`, `fleet`, `driver`, `route`, `dispatch`, `reporting`, `incident`, `maintenance`, `reconciliation`, `analytics`, and `audit`. Shared interface helpers and validation support remain under `common`.

### Feasibility and tradeoffs

The existing Java and PostgreSQL implementation provides a practical base for incremental development. A single deployment avoids the additional network and operational work of microservices. Relational constraints and transactions suit assignments that connect vehicles, drivers, route versions, incidents, and history.

Vaadin retains the existing interface and keeps most development within Java. It also introduces frontend dependency generation and server-side session requirements. A first start or a relevant dependency change may require a frontend build before the login page appears. Deployment capacity must account for application sessions and database workload rather than assuming an unlimited number of concurrent users.

Keeping both current state and historical records makes daily queries practical while preserving significant changes. It also requires transaction tests, migration care, and storage planning as history grows. The design is not a full event-sourcing system.

Rules are deterministic and inspectable. Their effectiveness depends on the selected rules and the quality of entered information. Adding Kafka, Redis, RabbitMQ, Kubernetes, another frontend framework, or unrelated infrastructure is not required by the current project scope.

### External services and operating constraints

The dashboard uses OpenStreetMap tiles and Open-Meteo weather without requiring an API key in the current prototype. Browser requests disclose the viewed map area or selected weather coordinates, together with normal connection metadata. The application does not intentionally send staff records or operational reports to these providers.

Open-Meteo's free hosted API is limited to noncommercial use and has request limits. The map caches weather by selected location for ten minutes per map instance and does not poll in the background. A commercial deployment needs an appropriate service arrangement. [Open-Meteo terms](https://open-meteo.com/en/terms).

OpenStreetMap's standard tile service requires attribution and normal caching and prohibits bulk downloading and offline prefetching. Provider availability is not guaranteed. [OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/).

No production hosting budget, service-level commitment, or proven capacity is established here. These need an actual deployment plan and measurements. The project also needs an explicit repository licensing decision before distribution; dependency licenses do not automatically license the application.

## 6. User manual

These procedures follow the labels in the current source. They describe expected behavior and do not replace execution of the manual test plan. Use a dedicated test environment for examples. Replace sample identifiers if they are already in use.

### Sign in and use the workspace

1. Open the address supplied by the administrator. Local development uses `http://localhost:8080`.
2. Enter the username and password supplied for your account and sign in. A new installation uses the administrator credentials configured during setup; there is no assumed `admin/admin` password.
3. Select a module from the sidebar. The available modules depend on your role.
4. Use the screen's search and filters to find records. The small refresh button reloads the displayed records using the current filters. It does not submit a form or run reconciliation.

Drag a column-header divider to widen a grid column. Use the horizontal scrollbar when the columns exceed the available width. Many record grids open details when you double-click a row. Supported dialogs can be moved by dragging their headers.

To reorder modules, drag a sidebar row or its dotted handle. Keyboard users can focus a handle and press `Alt+Up` or `Alt+Down`. The order is remembered for that account in that browser. Clearing browser storage resets it; reordering does not grant access to another module.

Operational dates use the application's service zone, normally `Asia/Manila`. Report time fields explicitly display that zone. Follow the zone shown in the field rather than assuming every screen uses the computer's local clock.

### Register a bus and record availability

1. Open `Fleet Availability` and click `Add Bus`.
2. Enter a unique `Unit Number`, such as `ARC-901`, a unique sample `Plate Number`, and a `Capacity` from 1 to 80.
3. Click `Save`. Find the new bus in the list and reopen it to check the saved values.
4. Click its `Status` action. Check `Administrative Status` separately from `Reported Availability`.
5. Set the availability supported by the information received, enter the `Reason for Availability Change`, and click `Save`.
6. Use `History` in the status dialog to inspect earlier availability changes.

New buses begin with `UNKNOWN` availability. Available states are `AVAILABLE`, `MAINTENANCE`, `INSPECTION_HOLD`, `OUT_OF_SERVICE`, and `UNKNOWN`. An administratively active bus must also satisfy availability, maintenance, and assignment checks before it can receive work.

Use `Edit` for identity details. Administrator archival removes a bus from new operational selection while retaining its record. It is different from temporarily recording the bus as unavailable.

### Register a driver and record availability

1. Open `Driver Management` and click `Register Driver`.
2. Enter a unique `Employee Number`, first and last name, `License Number`, `License Type`, and `License Expiry Date`.
3. Leave `Contact Number` blank if it is not needed. If entered, it must contain exactly 11 digits starting with `09`, for example `09691477344`.
4. Click `Save`, then find and reopen the driver to check the saved information.
5. Use `Status` to maintain employment status. Use `Availability` to record `AVAILABLE`, `UNAVAILABLE`, or `UNKNOWN`, enter a reason, and click `Record Availability`.
6. Use `History` in the availability dialog to review earlier changes.

New drivers begin with `UNKNOWN` availability. Dispatch requires an active, unarchived, available driver with the recorded professional qualification and a license valid for the applicable date. This check does not verify the license with an issuing authority. Employment status and trip assignment remain separate from availability.

### Create a route and a reusable schedule

1. Open `Route Schedule` and click `Create Route`.
2. Enter a unique `Route Code`, `Route Name`, and the relevant description and positive `Estimated Duration (min)`.
3. Set `Version Effective From` to the date the definition should begin to apply.
4. Use `Add Stop` to enter the stop names, sequence, and configured arrival minutes needed for the route. Click `Save`.
5. On the saved route, click `Schedules`. Enter `Schedule Name`, `Planned Departure`, and `Effective From`. Enter an optional `Effective To` only when the schedule has an end date.
6. Click `Create Schedule`. The schedule appears in the dialog and can be activated or deactivated there.

An end date labelled exclusive is the first date the version or schedule no longer applies. For example, an end date of 31 October means the interval ends before service on 31 October.

Use `Edit` when publishing a later route definition. Set its effective dates and save the new version. Existing assignments retain their earlier version reference. The application rejects version changes that conflict with protected historical assignments. Click `Cancel` to discard an unsaved edit.

A reusable schedule does not automatically create daily assignments. The current dashboard road map also does not automatically plot the route's saved stops.

### Create and operate an assignment

Before continuing, register the bus, driver, and route, record resource availability, and make sure the route version covers the service date.

1. Open `Dispatch & Assignment` and click `Create Assignment`.
2. Select the `Bus`, `Driver`, and `Route` using the available choices.
3. Set `Dispatch Date`. Select a matching `Configured Schedule (optional)` or choose an ad hoc `Departure Time`.
4. Check `Planned Duration (min)`. The route version can supply the initial value. Enter a positive duration so the assignment has a defined planned end.
5. Add relevant notes and click `Create Assignment`. The saved assignment begins as `SCHEDULED`.
6. When recording the start of the operation, click `Start` on that assignment. The system checks eligibility again and records `IN_PROGRESS`.
7. When recording completion, click `Complete`. To cancel a scheduled or in-progress assignment, use its `Cancel` action and complete any requested confirmation.

The application blocks unavailable or ineligible resources and overlapping bus or driver intervals, including intervals crossing midnight. Another trip already recorded as in progress for the same resource can also block a start. Correct the named problem or select another eligible resource; do not change unrelated data merely to bypass the check.

Planned times and recorded actual times are different. Starting or completing a trip records an application action; it does not independently observe the bus. Completed or cancelled assignments can be archived by an administrator.

### Record received operational information

Operational Reports preserves information received by staff before, or alongside, a separate operational decision. For example, a driver's radio statement about a flat tire can be recorded even before an administrator decides how to handle the bus or trip.

1. Open `Operational Reports` and click `Record Report`.
2. Select `Source`: `STAFF_ENTRY`, `PHONE`, or `RADIO`.
3. Select `Affected Record Type`, then search and select the actual bus, driver, route, or dispatch in `Affected Record`. For a bus, search its unit or plate number.
4. Set `Effective At (Asia/Manila)` to when the reported condition applied. This can differ from the time you enter it.
5. Enter `Reported Information`, such as `The driver of ARC-901 reported a flat tire by radio near the terminal.`
6. Click `Record`. Reopen the report and inspect its source, affected record, and recorded times.

Reports begin as `UNVERIFIED`. An administrator uses `Review`, selects `CONFIRMED`, `DISPUTED`, or `REJECTED`, enters `Evidence and Reason`, and clicks `Save Review`. Use `History` to inspect prior review decisions.

Freshness is calculated from effective time. Default thresholds are 30 minutes for `AGING` and two hours for `STALE`; these are configurable. Neither a review decision nor a freshness label automatically updates bus availability, changes a trip, or proves the report's physical truth.

### Record and resolve an incident

This screen currently requires an administrator.

1. Open `Incident Management` and click `Report Incident`.
2. Enter the `Title`, description, and `Severity`. The current choices are `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL`; record the assessed priority without treating the label as independently measured harm.
3. Select `Affected Service` when applicable. For `FLEET`, search and select the bus in `Affected Bus`. Other affected types currently use the actual numeric `Affected Record ID`.
4. If the incident comes from an existing report, enter that report's actual numeric `Operational Report ID`. Set `Effective At (Asia/Manila)` and click `Report`.
5. Use the available actions in order: `Acknowledge`, `Investigate`, `Resolve`, and `Close`. Enter resolution notes when requested.
6. Inspect `History` to review the recorded changes. A closed incident appears in `Archived Data` under `Archived Incidents`.

The current lifecycle is `REPORTED -> ACKNOWLEDGED -> INVESTIGATING -> RESOLVED -> CLOSED`. A resolved incident remains on the working screen until the explicit Close step.

Where a field explicitly asks for an ID, use the database ID shown in that saved record's details. The digits in `ARC-901` are not its database ID. For a bus or trip incident, the available maintenance-link action can connect work for the same vehicle. Unlinking requires a reason and preserves both records and their history. It does not cancel the maintenance work.

### Request and complete maintenance

This screen currently requires an administrator.

1. Open `Maintenance Management` and click `Request Maintenance`.
2. Search and select the `Bus`. Select `Maintenance Type`, enter the description, and set the scheduled date when applicable.
3. Click `Request`. Review the saved work request.
4. Use `Approve`, then `Start Work`, as the work progresses. Use `Cancel` only where that action is available for the current state.
5. Click `Complete` when recording finished work and enter `Technician Notes` as requested.
6. Open `Archived Data` and select `Archived Maintenance` to inspect the completed record and its history.

The normal lifecycle is `REQUESTED -> APPROVED -> IN_PROGRESS -> COMPLETED`, with controlled cancellation. Nonterminal maintenance blocks new assignments. Completing the last maintenance hold leaves availability `UNKNOWN` until staff record a new decision. Other holds remain effective; completing a work record does not certify the bus as roadworthy.

### Review reconciliation exceptions

This screen currently requires an administrator.

1. Open `Exception Queue` and click `Run Reconciliation Engine`.
2. Review the resulting cases. Open the affected record details and identify the rule, source records, and time detected.
3. Use `Acknowledge` and `Investigate` as the review proceeds.
4. Correct an incorrect source record through its own module when justified. Then use the available `Resolve` or `Dismiss` action and enter the requested explanation.
5. Run reconciliation again when needed and inspect both the case and source records.

Supported checks include defined resource/assignment contradictions, maintenance conflicts, aging operations, and report-review conditions. Repeated scans should not duplicate an unchanged unresolved rule-and-record case. Resolving a case does not certify physical conditions or automatically perform a source correction.

Development-only simulation controls may appear when the `dev` profile is enabled. Use them only with disposable test data. A simulated incident or contradiction is test input, not an operational event observed in a real fleet.

### Use archives and audit history

Both screens currently require an administrator.

1. Open `Archived Data` and choose the tab for buses, drivers, routes, dispatches, users, incidents, maintenance, or operational reports.
2. Open the record details or `Details & History` where provided.
3. Use `Restore` where the record supports it. Restore an operational report before reviewing it again.
4. Use `Delete Permanently` only when offered and when deletion is intended. Read the confirmation and any explanation about linked records before proceeding.

Archive behavior depends on the record type. Completed maintenance and closed incidents move automatically to their archive tabs. Archiving an account disables it. Archiving an operational report retains its verification status but removes it from active report lists, dashboard report counts, and new reconciliation scans.

Permanent deletion is guarded by record-specific rules and is not available for every archive type. Linked operational records can block deletion. Successful supported deletions retain the required historical events or audit snapshots. Administrators cannot bypass protected references simply by using the archive screen.

To inspect a protected action, open `Audit Log`, enter any relevant `Actor`, `Action`, or `Entity type`, select an `Outcome` if needed, and click `Search`. Double-click a result for its details. This screen shows application actions and outcomes; business history remains available with the operational record.

### Manage staff accounts

1. As an administrator, open `User Administration` and click `Create Staff Account`.
2. Enter the account details, role, and password, then click `Save`.
3. Use `Edit Account` to change the username, display name, optional email, or role. Click `Save Changes` to apply the edit.
4. Use `Reset Password` when a new password is required. Use `Archive Account` when access should be removed and the account retained in the archive.

Created and reset staff passwords must contain 8 to 16 printable ASCII characters, including an uppercase letter, a lowercase letter, a number, and a symbol. Spaces and non-ASCII characters are not accepted. The estimated strength label is advisory; server-side rules determine acceptance.

Account edits retain the account ID and password unless a separate reset is performed. Previous usernames remain reserved for historical attribution. Changing your own username or role signs out the editing session. Current/default administrator protections and grantor references can restrict archive deletion.

### Read the dashboard and external reference map

Open `Dashboard Overview` to review the latest information saved in the application. Use the underlying operational modules to investigate a count or condition. A summary does not establish that all manual information is current or physically verified.

The road map is centered on Cavite. Pan or zoom, select a town, or click a map point to request weather for that location. The weather panel shows temperature, wind, precipitation, and the maximum forecast rain probability over the next three hourly readings, together with model time and attribution. These are external model values, not verified roadside observations.

Weather failure should leave the road map usable. Missing tiles or weather may indicate an internet or provider problem. The panel does not change saved operations, show live bus positions, calculate an automatic ETA, or provide a traffic feed.

### Handle errors and sign out

If a form reports invalid input, read the named field or conflict, correct it, and submit again. If you are unsure whether a save succeeded, refresh and reopen the record before repeating the action. Clicking `Cancel` closes an unsaved form without applying its edits.

If a permission error appears, check the signed-in role with an administrator. If an unexpected error occurs, record the screen, attempted action, time, visible message, and sample record identifier. Give these details to the maintainer without including a password or session secret.

When finished, click `Logout` in the sidebar. The application should return to the login screen. Do not use a shared signed-in session as a substitute for individual staff accounts.

## 7. Installation and setup

### Prerequisites

Use JDK 21, the included Maven Wrapper, and PostgreSQL 17. Docker with Compose can provide the local database; a local PostgreSQL installation is an alternative. Maven and Vaadin need internet access when resolving uncached Java and frontend dependencies. A separate system Maven installation is not required when using the wrapper.

The commands below use PowerShell from the existing checkout. They do not reset an existing database or replace its data.

### Configure a local installation

1. Open PowerShell in the project directory:

   ```powershell
   Set-Location 'C:\projects\arc-transit'
   ```

2. Select an installed JDK 21. Replace the sample path if your installation differs:

   ```powershell
   $env:ARC_JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
   .\mvnw.cmd -version
   ```

   Confirm that the Maven output reports Java 21. The Windows wrapper can use `ARC_JAVA_HOME`, `JAVA_HOME`, or a supported local JDK location. Configure the IDE's Maven runner for JDK 21 as well.

3. If `.env` does not already exist, copy the example:

   ```powershell
   if (-not (Test-Path -LiteralPath '.env')) {
       Copy-Item -LiteralPath '.env.example' -Destination '.env'
   }
   ```

4. Edit `.env` and set the values below. Replace the placeholders with your own credentials; do not use the placeholder text as a password.

   ```properties
   POSTGRES_DB=arc_transit
   POSTGRES_USER=arc_application
   POSTGRES_PASSWORD=<your-database-password>
   ARC_SECURITY_ADMIN_USERNAME=admin
   ARC_SECURITY_ADMIN_PASSWORD=<your-initial-administrator-password>
   ```

   Keep `.env` out of version control. The file is read as environment configuration by Compose and imported as properties by the application, so avoid ambiguous quoting or escape characters. Existing process environment variables can override file values. Do not paste credentials into test evidence or issue reports.

5. Start the development database:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

   The development database is exposed only on `127.0.0.1:5433`; PostgreSQL uses port 5432 inside its container. Wait for the database to become healthy. If using a local server instead, configure the connection for that server and skip the Compose command.

6. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On first startup, Flyway creates the configured schema and applies its migrations, then Hibernate validates the mappings. The frontend may need to build before login becomes available. Review the terminal for a successful startup or the specific failure.

7. Open `http://localhost:8080` and sign in with the configured administrator account. Use `User Administration` to create the staff accounts needed for testing.

Administrator bootstrap creates the initial account when no administrator exists and the configured username is not already present. It does not promote an existing account or reset its password on restart. Changing the bootstrap password in `.env` is therefore not a password-reset procedure for an existing account.

Use `Ctrl+C` in the application's terminal to stop the application. `docker compose stop postgres` stops the development database while retaining its volume. Do not remove a database volume as a routine troubleshooting step.

### Configuration reference

| Setting | Purpose and default behavior |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database identity and credentials; the password must be supplied |
| `POSTGRES_HOST`, `POSTGRES_PORT` | Local application connection defaults to `localhost:5433` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | Override the application's JDBC connection and username |
| `ARC_SECURITY_ADMIN_USERNAME`, `ARC_SECURITY_ADMIN_PASSWORD` | Initial administrator bootstrap configuration |
| `SPRING_PROFILES_ACTIVE` | Profiles are opt-in; `dev` exposes development helpers and `prod` uses deployment settings |
| `arc.service-zone` | Service-date calculations default to `Asia/Manila`; some report fields explicitly use that zone |
| `arc.reporting.aging-after`, `arc.reporting.stale-after` | Report freshness defaults to 30 minutes and two hours |
| `arc.reconciliation.dispatch-staleness-threshold` | Default active-dispatch review threshold is two hours |
| `arc.reconciliation.incident-resolution-deadline` | Default incident review threshold is 24 hours |

Use Spring configuration or startup arguments for dotted property names. For example, `--arc.reporting.aging-after=PT30M` sets a duration on a packaged application. Review all time-dependent workflows before changing the service zone; not every current field is zone-configurable.

### Database changes and existing installations

Flyway owns the schema under `arc`. The current migration sequence extends from V1 through V26. V1-V11 establish the original schema. Later migrations add separate availability/history, maintenance and dispatch events, route versions and schedules, reconciliation rule identity, report provenance, historical protections, username reservations, validation constraints, and archive behavior.

Before updating an existing installation, back up the database, record the application version, and inspect its Flyway history. Apply the new migrations in a test copy first and review any legacy records that need attention. Some migrations preserve older values while applying stronger constraints to new or changed records.

Do not edit an already-applied migration, erase Flyway history, or reset the database to avoid a migration error. Add a forward migration when an established schema needs a change. Confirm recovery by restoring a backup into a separate database; creating a backup file alone is insufficient evidence.

### Build and verification

Use a dedicated test database. The existing PostgreSQL-backed tests do not make the application's normal operational database safe for testing.

On Windows with PostgreSQL 17 binaries installed locally, the repository provides an isolated verification runner:

```powershell
.\scripts\verify-postgres.ps1 -MavenArguments @('test')
.\scripts\verify-postgres.ps1 -MavenArguments @('verify', '-Dvaadin.productionMode=true')
```

The script creates a temporary loopback-only PostgreSQL cluster under `.verification`, supplies temporary configuration, runs Maven, and stops its own cluster. Override `-PostgresBin` and `-JavaHome` when the installed paths differ. Retained logs and test data are local verification artifacts and may contain fixture or temporary configuration information. `-SmokeTest` additionally starts a copy of the built JAR for the script's startup check.

When an isolated test database has already been configured, the underlying build commands are:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Dvaadin.productionMode=true verify
```

`ARC_TEST_POSTGRES=true` enables the PostgreSQL workflow and migration tests. The verification script supplies it automatically. A successful build with database tests skipped is not equivalent to a full PostgreSQL verification run. Inspect the actual test summary and logs.

The client tests can be run with an available Node.js runtime:

```powershell
node --test tests/client/*.test.cjs
```

Run `clean` separately if needed; do not pass it inside the isolated runner. If Windows reports that the built JAR is in use, stop the application that is running from that JAR, then retry. Do not stop unrelated Java processes or suppress the clean failure.

The repository's [test guide](tests/README.md), [input-validation guide](docs/INPUT_VALIDATION.md), [PostgreSQL CRUD guide](docs/POSTGRES_CRUD_TESTING.md), and [current manual test document](docs/Arc-Transit_QA_TEST.docx) provide supporting procedures. Direct SQL fixture scripts bypass application authorization and some business rules; use them only in the dedicated test environment described by the guide.

### Deployment configuration

The repository includes a Dockerfile and a production Compose configuration for one application and PostgreSQL. Build the production frontend and JAR, then start that configuration:

```powershell
.\mvnw.cmd -Dvaadin.productionMode=true verify
docker compose -p arc-transit-prod -f docker-compose.prod.yaml up --build -d
```

Run the build with test database configuration in place. The production Compose file does not expose PostgreSQL to the host and binds the application to `127.0.0.1:8080`. Its `prod` profile enables secure session cookies. Configure the intended HTTPS endpoint, TLS termination, and trusted proxy behavior before using deployed browser sessions.

Both Compose files declare a volume named `postgres_data`. The explicit `arc-transit-prod` project name above separates a new deployment from the default development Compose project. For an existing deployment, use its established project name and volume configuration; changing the project name can select a different database volume. Confirm the intended database before starting either configuration.

Build success, Docker image execution, HTTPS configuration, migration execution, browser smoke tests, and backup restoration require separate evidence. The presence of configuration files does not mean the deployment has passed those checks.

### Troubleshooting

| Symptom | What to check |
| --- | --- |
| Maven reports the wrong Java version | Set `ARC_JAVA_HOME` to an installed JDK 21, use the wrapper, and check the IDE Maven runner |
| Database connection fails | Check database health, credentials, host, port, and environment overrides; local development defaults to port 5433 |
| Changed database password has no effect | An existing PostgreSQL volume retains its initialized credentials; changing `.env` does not reset them |
| Startup requires an administrator password | Supply the bootstrap password for the initial administrator when no administrator exists |
| Existing administrator cannot sign in after `.env` changes | Bootstrap does not reset existing credentials; use the supported account-reset workflow |
| Frontend build screen appears | Allow the first or changed-dependency build to finish and inspect the first actual Vite error if it fails |
| A running JAR prevents `clean` | Stop that application through its terminal or IDE, then rerun `clean` |
| Flyway migration fails | Record the exact error, preserve the database and history, and investigate against a backed-up test copy |
| An entered field still reports that it is required | Record the screen and whether the value was typed or pasted; reproduce the unresolved browser submission issue rather than marking it passed |
| Map or weather is unavailable | Check internet access and the provider response; these external references are separate from saved operational records |

## 8. Future implementation

### Complete the current evaluation

The immediate priority is to finish and record the existing manual walkthrough, reproduce the unresolved browser submission issue, fix confirmed defects, and retest affected workflows. Run a complete connected scenario after the fixes, including both roles and rejected actions. Preserve actual outcomes rather than treating an expected result as a passing test.

Next, measure reconciliation accuracy, assignment validation, record/history consistency, report completeness, audit completeness, database queries, and application performance using documented datasets. Test backup restoration and the intended deployment. These steps establish evidence for the existing project rather than adding features solely to increase its size.

Automated checks should eventually run for proposed changes and before deployment. A future CI workflow can cover compilation, unit and PostgreSQL tests, module boundaries, client tests, frontend packaging, and selected dependency checks. Post-deployment checks should use safe health and smoke checks. This README does not claim that a GitHub CI/CD pipeline or production monitoring service is already configured.

### Clarify workflow and permission decisions

The following proposal items require explicit requirements and implementation decisions:

- Whether a planned trip can exist before a bus and driver are assigned.
- Whether dispatch needs separate `PLANNED`, `ASSIGNED`, `DISPATCHED`, and `IN_SERVICE` stages, plus curtailment, diversion, or replacement links, beyond the current four states.
- Whether operations staff should gain incident, maintenance, reconciliation, or archive permissions, or whether narrower dispatcher, maintenance, supervisor, and auditor roles are needed.
- Whether report verification needs a distinct supervisor workflow or an explicit superseded outcome beyond the current review choices.
- Which additional reconciliation rules, freshness indicators, configurable reasons, and causal links are required by an actual operator.

These are differences to resolve against the latest proposal, not features to describe as already available. State or permission changes require migration and regression planning where they affect stored records or access.

### Supporting improvements

After the main workflows are stable, possible additions include clearer route-map presentation, supporting-document attachments, controlled CSV import/export, additional operational summaries, notifications, a limited integration API, and GTFS Schedule export. Each needs a defined user need, data contract, permission model, and acceptance test before implementation.

Maintainability work should follow demonstrated needs: query and index improvements, better diagnostic logs without secrets, accessibility and responsive-layout corrections, retention and recovery procedures, dependency maintenance, and documentation matching the implemented behavior. Preserve the existing application design and working module boundaries while making those changes.

### Integrations outside the present scope

GPS/AVL feeds, live telemetry, automatic ETA, GTFS Realtime, passenger-information feeds, predictive maintenance, mathematical scheduling optimization, multi-tenant operation, and offline synchronization would require additional sources, infrastructure, validation, and evaluation. They remain future integration possibilities requiring a separate scope decision.

Any future external source should retain its provenance, timestamps, and verification limitations. It should not silently replace staff-entered history or convert a recorded assertion into proven physical truth.

The remaining documentation is to be prepared separately: Development Progress Tracker, Software Requirements Specification, System Design, and QA and Test Plan. Their detailed content and templates remain subject to review before each document is created.
