> Historical manual record. Earlier statuses and expectations have not been reverified for the redeveloped application. Use tests/README.md and CODEX_PROGRESS.md for current procedures and evidence.

# Feature & Functionality Test Results

## 1. Fleet Management CRUD

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-01 | Create fleet unit | Fill form: unit number, plate, type (BUS), capacity | Unit appears in grid with status ACTIVE |  |
| FT-02 | Duplicate unit number | Create unit with same number | Error: "Unit number already exists" |  |
| FT-03 | Edit fleet unit | Change plate number, save | Updated value persists after refresh |  |
| FT-04 | Change status | Set unit to OUT_OF_SERVICE | Status column updates |  |
| FT-05 | Archive unit | Click Archive | Unit disappears from main grid, appears in Archive view |  |
| FT-06 | Unarchive unit | Go to Archives > Buses > Unarchive | Unit returns to Fleet grid as ACTIVE |  |
| FT-07 | Hard delete | Archives > Permanently Delete | Unit removed from database entirely |  |

## 2. Driver Management CRUD

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-08 | Create driver | Fill: employee #, name, license type/number, expiry date | Driver appears in grid |  |
| FT-09 | Duplicate employee # | Create driver with same employee # | Error: "Employee number already exists" |  |
| FT-10 | Expired license detection | Set expiry date to past | "EXPIRED" badge renders in grid |  |
| FT-11 | Archive driver | Click Archive | Driver moves to Archives |  |

## 3. Route Management CRUD

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-12 | Create route with stops | Fill route info, add 3 ordered stops | Route appears with stop count = 3 |  |
| FT-13 | Edit route stops | Remove 1 stop, add 2 new stops | Stop count updates to 4 |  |
| FT-14 | Delete stop inline | Click trash icon on a stop | Stop removed from list before save |  |
| FT-15 | Archive route | Click Archive | Route moves to Archives |  |

## 4. Dispatch Assignment & State Machine

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-16 | Create assignment | Select active bus, active driver, active route, set date/time | Assignment created with status SCHEDULED |  |
| FT-17 | Bus overlap rejection | Assign same bus to overlapping time | Error: bus already assigned |  |
| FT-18 | Driver overlap rejection | Assign same driver to overlapping time | Error: driver already assigned |  |
| FT-19 | Inactive bus rejection | Try assigning archived/maintenance bus | Error: unit not ACTIVE |  |
| FT-20 | Expired license rejection | Try assigning driver with expired license | Error: license expired |  |
| FT-21 | Start trip | Click "Start Trip" on SCHEDULED | Status changes to IN_PROGRESS |  |
| FT-22 | Complete trip | Click "Complete Trip" on IN_PROGRESS | Status changes to COMPLETED |  |
| FT-23 | Cancel assignment | Click "Cancel" on SCHEDULED | Status changes to CANCELLED |  |

## 5. Maintenance State Machine

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-24 | Request maintenance | Select unit, type, notes | Record created with status REQUESTED |  |
| FT-25 | Approve maintenance | Click Approve | Status changes to APPROVED |  |
| FT-26 | Start maintenance | Click Start | Status: IN_PROGRESS, fleet unit: UNDER_MAINTENANCE |  |
| FT-27 | Fleet status sync | Check Fleet view while maintenance active | Unit shows UNDER_MAINTENANCE |  |
| FT-28 | Complete maintenance | Click Complete | Status: COMPLETED, fleet unit returns to ACTIVE |  |

## 6. Reconciliation Engine

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-29 | Simulate contradiction | Exception Queue > Simulate Contradiction | Success notification |  |
| FT-30 | Simulate stuck trip | Exception Queue > Simulate Stuck Trip | Success notification |  |
| FT-31 | Run reconciliation | Click "Run Reconciliation Engine" | Exceptions appear in grid with severity |  |
| FT-32 | Acknowledge exception | Click Acknowledge on OPEN exception | Status: ACKNOWLEDGED |  |
| FT-33 | Investigate exception | Click Investigate on ACKNOWLEDGED | Status: INVESTIGATING |  |
| FT-34 | Resolve exception | Click Resolve, enter notes | Status: RESOLVED |  |
| FT-35 | Dismiss exception | Click Dismiss, enter notes | Status: DISMISSED |  |

## 7. Security & Authentication

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| FT-36 | Create staff account | Admin > User Admin > Create (OPERATIONS_STAFF role) | Account created, can login |  |
| FT-37 | Staff view restriction | Login as staff, try `/admin/users` | 403 Access Denied |  |
| FT-38 | Password hashing | Check database `arc.app_users` | Password stored as BCrypt hash, not plaintext |  |
| FT-39 | Delete admin protection | Try deleting the only admin | Error: "Cannot delete the only remaining SYSTEM_ADMIN" |  |
| FT-40 | Non-master admin delete block | Login as secondary admin, try deleting another admin | Error: "Only the Default Admin is authorized" |  |
| FT-41 | Audit log capture | Login, create a fleet unit, check `arc.audit_logs` | Login event + CREATE action recorded |  |
| FT-42 | Session timeout | Leave app idle, return after session expires | Redirected to login |  |

## 8. Automated Test Suite

| # | Tests | Command | Status |
|---|-----------|---------|--------|
| FT-43 | Full suite | `mvnw clean test` | |
| FT-44 | Modularity | `mvnw -Dtest=ModularityTests test` | |
| FT-45 | Authentication | `mvnw -Dtest=AuthenticationTests test` | |
| FT-46 | Fleet service | `mvnw -Dtest=AppFleetManagementServiceTest test` | |
| FT-47 | Driver service | `mvnw -Dtest=AppDriverManagementServiceTest test` | |
| FT-48 | Route service | `mvnw -Dtest=AppRouteManagementServiceTest test` | |
| FT-49 | Dispatch service | `mvnw -Dtest=AppDispatchServiceTest test` | |
| FT-50 | Context loads | `mvnw -Dtest=ArcTransitSystemApplicationTests test` | |
