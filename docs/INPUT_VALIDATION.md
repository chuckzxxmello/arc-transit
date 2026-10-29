# Input limits and validation

New and reset staff passwords accept 8 through 16 printable ASCII characters and require uppercase, lowercase, a digit and punctuation. Spaces are rejected. The minimum-length message is "Password must be at least 8 characters long." The browser input stops at 16, while the server rejects longer values without truncating or storing them. Existing credentials remain usable at login. Strength is an advisory estimate (weak, moderate, strong, very strong), displayed only after typing. Common passwords and repeated whole blocks remain weak; a run of punctuation alone does not make an otherwise varied password weak.

## Field limits

Limits are maxima, not minimum required lengths. Required fields separately reject blank input. Existing schema limits are retained where narrower than the requested tiers.

| Module | Fields | Maximum characters |
| --- | --- | ---: |
| Users | Username | 50 |
| Users | Display name | 100 |
| Users | Optional email | 200 |
| Fleet | Unit number / plate number | 30 / 20 |
| Drivers | Employee number | 30 |
| Drivers | Optional contact number | Exactly 11 digits starting with 09; example 09691477344 |
| Drivers | First name / last name / licence number | 50 |
| Routes | Route code | 30 |
| Routes | Route name / stop name / schedule name | 100 |
| Incidents | Title | 200 |
| All operational forms | Descriptions, notes, reasons, reported information and review evidence | 500 |
| Management, dashboard and archive | Search text | 200 |
| Audit | Actor, action and entity filters | 100 |
| Record links | Positive record IDs | 19 digits, within Java Long range |
| Dropdowns | Typed search text | 100 |

New and edited bus capacities accept 1 through 80 in UI, command validation, domain logic and PostgreSQL V22. Existing oversized capacities are retained until explicitly corrected; unrelated status/archive updates remain possible. Other integer controls reject values outside their signed integer range and preserve their existing positive/nonnegative domain rules. Required dates and choices are validated before submission; domain-specific date, qualification, availability and workflow checks remain on the server.

UI limits apply to typing and paste through the native input maxlength setting. Java component setters and forged requests are separately checked by form submission guards and service constraints. Values are rejected rather than silently truncated on the server. Normal Unicode names, punctuation and multiline narrative text remain supported; unsupported control characters and broken surrogate sequences are rejected. JPA query parameters and Vaadin text components continue to handle literal input as data.

## Dropdowns and failure feedback

Dropdowns accept their configured options and support filtering; custom values are disabled. Required selections show invalid state when empty. Dispatch bus, driver and route selectors fetch sorted pages with server search rather than loading only the first 100 records. Archived records are excluded. Driver employment and licence validity are filtered before pagination. Availability, qualifications, scheduling conflicts and stale selections remain authoritative service checks when saving.

Validation messages exclude method paths and rejected values. Known business conflicts, missing resources and permission failures receive clear messages. Database constraint and optimistic concurrency failures receive safe recovery guidance. Unexpected exceptions receive a generic retry message and are logged on the server. The global Vaadin session handler covers uncaught UI operations, including lazy data loading; it does not suppress transaction rollback or change authorization.

The original text-limit changes require no migration. The later bus-capacity rule uses V22. Existing records and historical long text are retained.

Incident reports with affected service FLEET use an Affected Bus dropdown; maintenance requests use a required Bus dropdown. Search a unit number or plate and select an existing result. The stored link uses that bus's actual record ID. Custom values are disabled, archived buses are excluded by the existing search service, and changing incident type clears the previous selection. Other incident record types retain their labeled numeric ID field. Service validation still checks records when saving.

The fleet Status dialog uses one transactional Save for separate Administrative Status and Reported Availability fields. Changing availability requires a reason; changing only administrative status does not invent an availability event. A validation, persistence or audit failure rolls back both changes.

Double-click record details open in draggable, resizable dialogs in operational modules, Audit Log and all archive tabs. Create/edit/state/history/confirmation dialogs in those modules are also draggable. User Administration and Dashboard Overview retain their existing layouts. Browser dragging is a manual acceptance check; Java component tests establish configured properties and listeners.

## Manual regression procedure

Run against a test environment. Record build/version, browser and fixture IDs. Treat the following as NOT RUN until executed in that browser.

1. Open account creation and password reset. Confirm strength is initially hidden. Type a compliant password such as `Us3!****aB9#cD2$`; it should be accepted and estimated very strong. Enter fewer than 8 characters and confirm the minimum wording and red invalid state. Verify ordinary punctuation including # works. Clear the input and confirm strength disappears.
2. Paste 500 repeated characters into password creation/reset. Confirm only 16 remain in the browser control. Confirm the separate server regression rejects a value longer than 16. Do not use this example password for a real account.
3. In each module's create and edit forms, type or paste beyond the applicable limit above. Confirm typing/paste stops at the limit, the accepted boundary saves with otherwise valid data, and there is no raw server/SQL error. Check route stops and schedules, availability reasons, incident resolution, technician completion notes, report review and exception resolution/dismissal as well as primary creation forms.
4. Submit empty required fields and clear required dropdowns. Confirm red field feedback, an open dialog and no saved mutation. Check that optional descriptions, contacts, schedule end dates and maintenance scheduled dates can still be blank where allowed.
5. Enter 0, negative, nonnumeric and out-of-range record IDs or capacities. Confirm rejection and no linked/saved wrong record. Include a 19-digit value above 9223372036854775807 for a record link.
6. Create more than 100 eligible resources in a disposable test database. Search for and select records beyond the first 100. Verify archived, inactive or expired drivers are not offered as eligible drivers. Change a resource after opening a dispatch form and confirm saving rechecks its current state.
7. Exercise duplicate identifiers, conflicting dispatch windows, an outdated record, unauthorized actions and a simulated database outage. Confirm actionable safe messages, no stack traces or internal SQL in notifications, preserved form input for correction and transactional rollback. Restore the environment and retry valid submissions.
8. Recheck archiving users: the account disappears from User Administration, is disabled, appears under Archived Users, and retains its historical identity.

Automated evidence and any execution limitations are recorded in CODEX_PROGRESS.md. Component tests establish server-side component state and listeners; they do not establish actual browser keystroke/paste behavior.


## Account editing and archive deletion checks

- In User Administration, select an account and choose Edit Account. Confirm username, display name, optional email and access role are prefilled. Change all four, save and reopen. Verify the same account ID, password and status remain, with a USER_UPDATED audit event and a USER_ROLES_CHANGED event when roles change. Clear email and verify it stays empty after reopening. A profile-only save must preserve all existing roles.
- Try an existing username and a previous username belonging to a different account. Expect rejection without partial profile/role changes. Old usernames remain reserved; they do not continue to authenticate after a rename. The account may return to its own previous name.
- Open the same account in two editors. Save the first and then the second. The stale second editor must be rejected with guidance to reopen it. Invalid email, blank username/display name and an empty role choice must not save. Archival and last-active-administrator protections remain in force.
- Change your own username or role and verify the editing session signs out. Sign in with the updated current username and unchanged password. Check the effective role after signing in. Other existing sessions retain the application's existing authentication-session behavior.
- Create an unused test bus, archive it, and permanently delete it through Archived Data. Its automatic registration availability event and prior audit events must not block deletion. The bus disappears, historical events remain and FLEET_UNIT_DELETED records its ID, unit number and plate.
- Try deleting a test bus with maintenance, dispatch, incident, operational report or reconciliation records. Expect a message naming the actual blockers and retain all records. Audit/history rows alone are not operational blockers. Simulate an audit write failure only in a disposable database and verify the deletion rolls back.

V20 through V26 apply through Flyway on application restart. These procedures must not be used to delete real operational records as test data.

## Columns, completed work and readable report records

- Drag the divider at the right edge of a grid column header to make the text readable. Try a long route name and a long report description. Widen several columns until they exceed the grid, then use its horizontal scrollbar to reach Actions. Repeat in an archive tab and a history popup.
- In Operational Reports, click Record Report at the right of the header. Choose Bus, type ARC-001 or its plate, and select the matching result. Do not enter 001 as a database ID. Select Phone or Radio as appropriate, enter when the information reportedly applied, and describe what was reported. The saved row should identify the bus as ARC-001. The report begins unverified and does not change the bus's availability.
- Try Driver, Route and Dispatch / trip. Search by the visible employee/name, route code/name, or trip bus/route/driver/date. Change record type after selecting a result: the old result must clear, and Record must reject a missing new selection.
- Complete maintenance with technician notes. It must disappear from Maintenance Management and appear in Archived Data -> Archived Maintenance, with notes and history intact. The final maintenance hold leaves availability UNKNOWN until staff record a new availability decision.
- Resolve an incident, then close it. RESOLVED remains visible until that explicit Close step. CLOSED moves to Archived Incidents with resolution notes and history intact. Double-click either archive type to read its details and history. Archive browsing remains administrator-only. Existing completed/closed records are classified by V25; pending work and cancelled maintenance remain unchanged.

## Refresh, navigation and corrected archive actions

- As an administrator, find a disposable operational report and click Actions -> Archive. Cancel first: it must remain active. Repeat and confirm Archive Report: it must disappear from Operational Reports and appear in Archived Data -> Archived Operational Reports. Its verification status, source, notes and Verification History must remain unchanged.
- In Archived Operational Reports, double-click the row or click Details & History. Restore it and verify it returns to the active module with its original verification status. Archive it again, click Delete Permanently and cancel: the record must remain. Repeat and confirm deletion only for the unused sample: it must disappear, while Audit Log retains OPERATIONAL_REPORT_DELETED with its snapshot and earlier audits remain.
- Try permanent deletion of an archived report linked to an incident or reconciliation exception. Expect a message naming the link; both records must remain. Resolve/close the linked case and repeat: its historical link still protects the report. An archived report cannot be reviewed until restored. Operations staff must not see Archive and cannot call archive/restore/delete services or open the administrator archive.

- Refresh beside a primary module button reloads displayed records and preserves current filters. It must not submit a form, run reconciliation, change status or delete records. The icon rotates on click unless reduced motion is enabled.
- Drag a sidebar row by its label, icon or dotted handle up or down, release at the highlighted placeholder, and confirm an ordinary click on its label still navigates. A small pointer movement must not reorder it. Try moving the last row to the top, dropping below the last row, scrolling at the sidebar edges and pressing Escape mid-drag. Focus a handle and press Alt+Up/Down for keyboard movement. Reload and sign in again with the same account to check saved order; another account retains its own order. Unknown paths and duplicate saved entries must not create modules or bypass authorization. No organizer dialog is needed.
- Open Dashboard Overview without any Google key: the Cavite road map should appear. Zoom and pan, select Tagaytay, and click another map location. Confirm the weather location and model time match the chosen point, missing weather fields say Unavailable, zero rain stays 0 mm, and the source attribution remains visible. Temporarily block the weather API in browser developer tools and reload: the road map must remain usable with a safe weather message. Reenable the API and retry. The weather refresh uses a ten-minute cache, so it need not fetch a new forecast on every click. No traffic feed or vehicle location is claimed.
- Try driver contact 09691477344, empty, +639691477344, 0969147734, 096914773444 and letters in registration and editing. Only the valid local number or an empty optional value should save. UI typing is capped at 11 digits; server validation also checks the full format. V24 preserves old values and guards inserts/contact changes in SQL.
- Archive an unused cancelled sample dispatch, then try Cancel in its permanent-delete confirmation. It must remain. Confirm deletion on a second attempt: the assignment disappears but its business events and DISPATCH_DELETED audit snapshot remain. Incidents, operational reports or reconciliation records linking the dispatch must block deletion.
- Archive a sample staff account and permanently delete it from Archived Users. Credentials and current role rows disappear, prior audit actors remain, and USER_DELETED identifies the removed account. Reusing any former username must fail. Accounts that granted roles to other users, the current account and the configured Default Admin remain protected. Use only disposable sample accounts for these checks.
- On a linked incident, choose Unlink Maintenance. Blank/spaces-only reasons and stale expected links must not save. A valid reason clears the current link and adds history/audit evidence while preserving the maintenance record and existing incident state. Cancel must leave the link unchanged. Link the matching maintenance again to confirm the workflow remains available.
