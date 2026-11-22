> Historical manual record. Earlier statuses and expectations have not been reverified for the redeveloped application. Use tests/README.md and CODEX_PROGRESS.md for current procedures and evidence.

# UI / Interface Test Results

## 1. Login Page

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-01 | Login page loads | Navigate to `http://localhost:8080` | Login form renders with username, password fields, and submit button | Pass |
| UI-02 | Valid login | Enter `admin` / `ArcTransit123`, click Login | Redirect to Dashboard | Pass |
| UI-03 | Invalid login | Enter `admin` / `wrongpassword`, click Login | Error message displayed, no redirect | Pass |
| UI-04 | Empty fields | Click Login with empty fields | Validation prevents submission | Pass |
| UI-05 | Page load time | Measure time from navigation to full render | < 3 seconds | Pass |

## 2. Navigation & Layout

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-06 | Sidebar renders | Login as admin | All 10 nav items visible (Dashboard, Dispatch, Fleet Monitor, Driver, Route, Incident, Maintenance, Exception Queue, Archived Data, User Admin) | Pass |
| UI-07 | Staff nav restriction | Login as OPERATIONS_STAFF | Only Dashboard and Dispatch visible | Pass |
| UI-08 | Clock display | Check header bar | Real-time clock shows current date/time | Pass |
| UI-09 | Logout | Click Logout in sidebar | Redirect to login page, session cleared | Pass |
| UI-10 | Active nav highlight | Click each nav item | Clicked item is visually highlighted | Pass |

## 3. Dashboard View

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-11 | Dashboard loads | Navigate to Dashboard | Summary cards, fleet table, and map render | Pass   |
| UI-12 | Summary cards data | Check card values | Cards show live counts (Active Fleet, Drivers, Routes, Dispatches) | Pass |
| UI-13 | Card click navigation | Click "Active Fleet Units" card | Navigates to Fleet Management view | Pass   |
| 14    | Map loads | Check map iframe | OpenStreetMap (Cavite region) renders | Pass |

## 4. Grid Views (Fleet, Driver, Route, Dispatch)

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-15 | Fleet grid renders | Navigate to Fleet Management | Grid with columns: Unit Number, Type, Status, Plate Number | Pass |
| UI-16 | Driver grid renders | Navigate to Driver Management | Grid with columns: Employee #, Name, License, Status, Expiry | Pass |
| UI-17 | Route grid renders | Navigate to Route Schedule | Grid with columns: Code, Name, Status, Stop Count | Pass |
| UI-18 | Dispatch grid renders | Navigate to Dispatch & Assignment | Grid with columns: Bus, Driver, Route, Date, Status | Pass |
| UI-19 | Expired license badge | Add driver with past expiry date | Red "EXPIRED" badge visible in grid |  |
| UI-20 | Grid pagination | Add 20+ records to any module | Grid scrolls or paginates correctly |  |

## 5. Error Handling Views

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-21 | 404 page | Navigate to `/nonexistent` | Custom 404 page with "Go Back to Dashboard" button |        |
| UI-22 | 403 page | Staff user navigates to `/admin/users` | Custom 403 Access Denied page |  |
| UI-23 | 404 button works | Click "Go Back to Dashboard" on 404 | Returns to Dashboard |  |

## 6. Dialog / Modal Forms

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| UI-24 | Create dialog opens | Click "Add" button on any management view | Modal dialog with form fields |  |
| UI-25 | Dialog cancel | Click Cancel on dialog | Dialog closes, no data changed |  |
| UI-26 | Route stop editing | Open route create dialog, add 3 stops | Stops appear as editable list with trash icons |  |
| UI-27 | Confirm dialog | Click "Delete" on any entity | ConfirmDialog appears with warning text |  |
