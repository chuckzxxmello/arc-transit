---
title: "Arc Transit: Transit Operations Coordination and Operational-State Reconciliation Platform"
aliases:
  - Arc Transit Operational-State Reconciliation
tags:
  - arc-transit
  - enterprise-software
  - public-transport
  - systems-engineering
  - philippines
  - operations
  - research
status: final-draft
date_created: 2026-10-04
date_researched: 2026-10-04
last_reviewed: 2026-10-05
source_window:
---

# Arc Transit: Transit Operations Coordination and Fleet Intelligence Organization Platform

A web-based transit-operations coordination application that supports fleet availability, driver availability, route definitions, dispatch assignments, incident handling, maintenance state, operational reporting, audit history, provenance, and rule-based reconciliation using primarily human-entered operational data.

---

# I. INTRODUCTION

The reviewed Philippine public-transport contexts and documentations regarding existing related application systems, shows that digital dispatch, GPS and telematics, workforce management, service scheduling, operations control, maintenance, passenger information, and data analytics are already established application system capabilities. The proposed project therefore does not claim novelty in these functions. Instead, it focuses on an operational-record and coordination model that remains usable when human-user data type of input information is an important source of operational state.

# II. OBJECTIVE

To design, develop and evaluate an operational-records and coordination system prototype that relies primarily on human-user operational state data input rather than automatically derived from connected vehicle telemetry.

Arc Transit's core capabilities:

1.  Fleet and Driver master records
2. Route and Route-version records with effective periods
3. Trip and Dispatch assignment lifecycle
4. Incident and Maintenance workflows
5. Manual operational reports with provenance
6. Current-state projection with append-only event history
7. Rule-Based Validation and Reconciliation
8. RBAC and Audit logging
9. A descriptive operational-monitoring dashboard
10. Controlled scenario test harnesses
11. Measurable software-level evaluation

---

# III. SCOPE AND LIMITATION

## 3.1 Scope

This project focuses on the internal operations of Philippine public bus services. The following source categories were prioritized to identify operational constraints, existing responses, and defensible opportunities for software support.

1. Philippine government and official legal sources.
2. PIDS/SERP-P and institutional research.
3. Current Philippine public-information reporting tied to government actions.
4. Official product/vendor documentation for existing-system capabilities.
5. International transport-system documentation only for architectural comparison.

The evidence-screening questions are:

→ Is the source accessible?
→ Does the page itself contain the supporting text?
→ Is the statement Philippine evidence or international reference?
→ Is it current, latest or historical, policy, industry, or case-specific?
→ Does the source directly establish the claim?
→ What does the source not establish?
→ Can the findings be translated into a software requirement without implying that software alone can solve institutional problems?

## 3.2 Limitations

→ Public online sources cannot establish the internal processes of every Philippine bus company.
→ Vendor websites only establish documented capabilities, not the complete inventory of every configuration.
→ Absence of a feature from a public vendor page is not proof that the product cannot provide it.
→ Pricing was deliberately excluded unless a reliable Philippine-specific quotation was available.
→ No operator interview or live deployment data was available.
→ The project should focus on software evaluation and should not be presented as evidence of transport-system outcomes beyond the defined software evaluation.

Excluded capabilities and unsupported claims:

→ automatic GPS or AVL location
→ telemetry-derived ETA
→ predictive maintenance
→ AI or machine-learning anomaly detection
→ autonomous dispatch
→ city-wide route optimization
→ passenger mobile app
→ fare and payment processing
→ claims of cheaper procurement
→ claims that the application independently proves real-world truth

## THE PROJECT DOES NOT CLAIM THIS

1. "The proposed system is cheaper than Samsara or Geotab in the Philippines."
2. "Samsara or Geotab cannot perform cross-module consistency checks."
3. "No existing system does reconciliation."
4. "Philippine transit operations are generally manual."
5. "All Philippine bus operators suffer from fragmented fleet ownership."
6. "All PUVs are required to use the same GPS configuration represented in a specific program."
7. "The proposed system verifies real-world truth."
8. "Append-only audit logs make reports tamper-proof in an absolute sense."
9. "The proposed system improves road safety" without a real safety-outcome study.
10. "The proposed system reduces congestion."
11. "The proposed system improves passenger travel time."
12. "The proposed system provides real-time vehicle tracking" without a live feed.
13. "The proposed system provides predictive maintenance" without diagnostic and training data.
14. "The proposed system uses AI" for deterministic if-then rules.
15. "The proposed system is a replacement for CAD/AVL, Optibus, HASTUS, TOPIS, Samsara, Geotab, INIT, or CleverCAD."
16. "Self-hosted means zero cost."
17. "Local deployment means full offline multi-user operation."
18. "RBAC and HTTPS prove Data Privacy Act compliance."

---

# IV. IDENTIFIED CONSTRAINTS

The strongest evidence findings are:

1. **The franchised fleet count does not necessarily reflect actual operating supply.** In September 2025, DOTr directed LTFRB to determine whether **3,972** PUVs franchised for Commonwealth Avenue routes were actually operational. This is case-specific evidence, not proof that every bus operator has the same problem.
2. **Service availability can change quickly because of external operating conditions.** In April 2026, DOTr stated that rising diesel prices had caused some PUV operators to reduce trips or stop operating.
3. **Routes are not static master data.** LTFRB modified **12 PUV routes** on January 29, 2026 and stated that hundreds of other routes were being studied.
4. **Dispatch is already an established Philippine operational-control function.** MMDA's Bus Management and Dispatch System was used to monitor the EDSA Busway and QR-coded driver records in 2023. The proposed system therefore, does not claim that dispatch digitization is novel.
5. **Consolidation and fleet management are established organizational mechanisms, but evidence is mixed and context-specific.** Research on the PUV modernization program documents consolidation, common fleet management, and differing cooperative experiences. Much of this evidence concerns jeepneys/cooperatives and must not be generalized to all bus operators.
6. **Human-entered monitoring is used in current government programs but has verification limitations.** The 2026 Service Contracting Program used both GPS and manual monitoring. In a separate official briefing, DOTr explained that manual monitoring made verification and validation difficult.
7. **Roadworthiness constrains usable fleet capacity.** Current LTFRB inspections explicitly check tires, lights, seats, and related conditions. The proposed system can record inspection and maintenance availability state, but cannot itself determine physical roadworthiness.
8. **Driver and staff records create privacy and security obligations.** Republic Act No. 10173 treats government-issued licenses as sensitive personal information and requires reasonable organizational, physical, and technical security measures.

This evidence supports the use of explicit operational state, provenance, historical records, validation, and exception handling. However, it does not establish that a nationwide Philippine central-record divergence problem exists in exactly the form described by INIT's ‘ghost bus’ example. INIT is therefore used as international architectural evidence rather than as evidence of Philippine operating conditions. INIT's documented field-versus-central-data problem provides a reasonable design basis for reconciliation features when combined with Philippine evidence showing mutable routes, changing fleet availability, manual reports, and mixed-source monitoring.

## 4.1 Authorized franchised fleet count can differ from actual operating supply

### CONSTRAINT

A franchise or master fleet record does not establish that a vehicle is currently available and operating.

### EVIDENCE

On September 25, 2025, the Philippine Information Agency reported that the acting Transportation Secretary directed LTFRB to determine whether **3,972 PUVs** with franchises for Commonwealth Avenue routes were actually operational.

### CONCRETE DATA

- Date: **September 25, 2025** (article publication date)
- Count: **3,972** franchised PUVs for Commonwealth routes
- Action: LTFRB instructed to determine whether they were actually operational
- Location: **Commonwealth Avenue, Quezon City**

### SOURCE

**Organization:** Philippine Information Agency (PIA)
**Title:** *DOTr chief wants more PUVs on Commonwealth Avenue* 
**Publication date:** 2025-09-25  
**Direct URL:** https://pia.gov.ph/news/dotr-chief-wants-more-puvs-on-commonwealth-avenue/  
**Source type:** Government public-information report  
**Evidence classification:** CURRENT DIRECT / CASE-SPECIFIC

### KEYWORDS

- `determine whether the 3,972 public utility vehicles issued franchises for Commonwealth routes are actually operational`

### INTERPRETATION

The operational system should not collapse:

- legal/administrative fleet membership
- current operational availability
- active service assignment

into one field such as `active = true`.

### SCOPE AND BOUNDARY

This source concerns the area of Commonwealth Avenue, Quezon City. It does **not** establish that all Philippine bus operators have inaccurate active-fleet records.

### SYSTEM IMPLICATION

Maintain separate concepts for:

- vehicle administrative lifecycle
- vehicle operational availability
- dispatch assignment
- service status

### POSSIBLE IMPLEMENTATION

- `fleet_unit`
- `vehicle_availability_state`
- `availability_effective_at`
- `availability_source`
- `dispatch_assignment`
- state/event history
- dashboard count of available vs unavailable vs assigned units

### LIMITATION

The proposed system can record an operator's assertion that a unit is available. Without inspection, telemetry, or another independent source, it cannot prove the unit is physically roadworthy or actually on the road.

---

## 4.2 Service supply can change because of external operating shocks

### CONSTRAINT

A planned service schedule can become infeasible because vehicles/operators reduce or stop trips in response to external operating conditions.

### EVIDENCE

On April 14, 2026, PIA reported the Transportation Secretary's statement that higher diesel prices had caused many PUV operators to limit trips, with some reducing runs and others stopping operations.

### CONCRETE DATA

- Date: April 14, 2026
- Program response: Service Contracting Program
- Government allocation reported: ₱1 billion
- Operational observation: some PUV operators reduced runs or stopped operating

### SOURCE

**Organization:** Philippine Information Agency  
**Title:** *Marcos Jr. gov’t launches P1-B service contracting April 15*  
**Publication date:** 2026-04-14  
**Direct URL:** https://pia.gov.ph/news/marcos-jr-govt-launches-p1-b-service-contracting-april-15/  
**Source type:** Government public-information report  
**Evidence classification:** CURRENT DIRECT / PROGRAM CONTEXT

### KEYWORDS

- `Some have reduced their runs and others have stopped operating entirely`

### INTERPRETATION

The proposed system should allow planned service and actual service availability to diverge without deleting the original plan.

### SCOPE AND BOUNDARY

The source describes a specific fuel-price shock and government response. It does not prove chronic cancellation levels or identify a universal bus-operator workflow.

### SYSTEM IMPLICATION

Store:

- planned trip
- actual dispatch state
- cancellation/curtailment reason
- effective timestamp
- actor/source

### POSSIBLE IMPLEMENTATION

Trip lifecycle:

`PLANNED → ASSIGNED → DISPATCHED → IN_SERVICE → COMPLETED`

Alternative exits:

`PLANNED or ASSIGNED → CANCELLED`

Possible disruption states/events:

`DELAY_REPORTED`, `CURTAILED`, `DIVERTED`

Reason categories should be configurable rather than hard-coded around fuel prices.

### LIMITATION

Software can record reduced service. However, it cannot solve fuel-price shocks, operator financing, or supply economics.

---

## 4.3 Route structures are mutable and need effective dating and version history

### CONSTRAINT

Routes can be officially modified while operations continue. Treating a route as a permanently fixed record can make old assignments difficult to interpret.

### EVIDENCE

LTFRB modified 12 PUV routes in Quezon City, Mandaluyong City, Rizal, and Surigao del Sur on January 29, 2026. The same report stated that hundreds of other routes were being studied.

### CONCRETE DATA

- Date: January 29, 2026
- Routes modified: 12
- Additional scope: hundreds of routes under study

### SOURCE

**Organization:** Philippine News Agency  
**Title:** *LTFRB ‘rationalizes’ 12 PUV routes in NCR, Rizal, Surigao Sur*  
**Publication date:** 2026-01-29  
**Direct URL:** https://www.pna.gov.ph/articles/1268016  
**Source type:** Government news agency report  
**Evidence classification:** CURRENT DIRECT / POLICY-IMPLEMENTATION

### KEYWORDS

- `modified 12 public utility vehicle (PUV) routes`
- `Hundreds of other PUV routes are being studied`

### INTERPRETATION

A route should have a stable identity while route definitions, stop sequences, or service parameters can change over time.

### SCOPE AND BOUNDARY

This does not establish how frequently every bus operator changes routes, nor does it prove that the proposed system must automate LTFRB route approvals.

### SYSTEM IMPLICATION

Use route versioning/effective periods rather than overwriting the route used by historical trips.

### POSSIBLE IMPLEMENTATION

- `route`
- `route_version`
- `effective_from`
- `effective_to`
- `route_stop_version`
- assignment references the applicable route version or immutable trip snapshot

### LIMITATION

The proposed system can represent route definitions and effective periods but cannot authorize a route or replace LTFRB's regulatory process.

---

## 4.4 Dispatch is an operational-control workflow, not merely a timetable field

### CONSTRAINT

Dispatch requires active monitoring of units/drivers and operational changes. It should be modeled as a workflow with actors, timestamps, assignment state, and validation.

### EVIDENCE

In June 2023, PIA reported that MMDA's Bus Management and Dispatch System was monitoring the EDSA Busway. Dispatching officers at PITX and Monumento could scan unique QR codes to access driver information including unsettled violations.

Historical PIDS work also documents earlier manual and common dispatch mechanisms and implementation problems. That historical evidence is **not** proof that the same problems persist in 2026.

### SOURCES

**Organization:** Philippine Information Agency / Department of Transportation information  
**Title:** *MMDA’s Bus Management and Dispatch System now monitors EDSA Busway*  
**Publication date:** 2023-06-19  
**Direct URL:** https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway  
**Evidence classification:** CURRENT DIRECT (within priority window) / EXISTING SYSTEM

**Organization:** Philippine Institute for Development Studies  
**Title:** *Competition for the Market: A Policy Framework for Improving Bus Operation along EDSA*  
**Publication date:** 2017-04-11  
**Direct URL:** https://www.pids.gov.ph/publication/discussion-papers/competition-for-the-market-a-policy-framework-for-improving-bus-operation-along-edsa  
**PDF:** https://pidswebs.pids.gov.ph/CDN/PUBLICATIONS/pidsdps1710.pdf  
**Evidence classification:** HISTORICAL / INSTITUTIONAL

### KEYWORDS

Philippine Information Agency / Department of Transportation information:
`systematic monitoring of the number of units plying the EDSA Busway`
`scan drivers' unique QR codes`

Philippine Institute for Development Studies:
`common dispatching service`
`Manual OBR unsuccessful`

### INTERPRETATION

The proposed system should treat dispatch as a transaction and workflow that combines vehicle, driver, trip, assignment time, lifecycle, change history rather than storing only a scheduled departure.

### SCOPE AND BOUNDARY

The proposed system must **not** claim to invent Philippine dispatch digitization. BMDS already demonstrates a real Philippine dispatch and monitoring system.

### SYSTEM IMPLICATION

- dispatch assignment entity
- pre-dispatch validation
- assignment lifecycle
- cancellation reason
- overlap detection
- actor and timestamp
- daily operations board

### LIMITATION

A prototype manual dispatch board is not equivalent to a production EDSA Busway control system or CAD/AVL platform.

---

## 4.5 Consolidation can create shared fleet-management needs, but evidence is context-specific

### CONSTRAINT

When individually managed PUV operations are consolidated into common organizations, shared fleet management, dispatch, resource assignment, and records become more important.

### EVIDENCE

A 2018 study indexed by PIDS/SERP-P found industry consolidation and fleet management significant components of the PUV modernization program. A 2024 study of cooperatives in NCR, Region IV-A, and Region VII reported diverse outcomes during consolidation and fleet modernization.

### SOURCES

**Organization:** UP School of Urban and Regional Planning; indexed by SERP-P/PIDS  
**Title:** *Examining the Potential Significance of Industry Consolidation and Fleet Management in Implementing the DOTr's PUV Modernization Program: A Case Study of 1TEAM*  
**Year:** 2018  
**Direct URL:** https://serp-p.pids.gov.ph/publication/public/view?slug=examining-the-potential-significance-of-industry-consolidation-and-fleet-management-in-implementing  
**Evidence classification:** HISTORICAL / INSTITUTIONAL / CASE STUDY

**Organization:** WeSolve Foundation; indexed by SERP-P/PIDS  
**Title:** *Assessing PUV Modernization Program Industry Consolidation and Financing toward a Just Transition for Transport Workers*  
**Year:** 2024  
**Direct URL:** https://serp-p.pids.gov.ph/publication/public/view?slug=assessing-puv-modernization-program-industry-consolidation-and-financing-towards-just-transition-for  
**Evidence classification:** CURRENT INSTITUTIONAL / CASE STUDIES

### KEYWORDS

UP School of Urban and Regional Planning:
`industry consolidation and fleet management are both significant components`

WeSolve Foundation; indexed by SERP-P/PIDS:
`show diverse experiences of cooperatives undergoing consolidation and fleet modernization`

### INTERPRETATION

A shared operational record is viable for a cooperative/organization managing multiple units and drivers.

### SCOPE AND BOUNDARY

Much of this evidence concerns **jeepney modernization and transport cooperatives**. It should be used only for the organizational mechanism such as consolidation/common fleet management, not as proof that all Philippine bus companies have the same conditions.

### SYSTEM IMPLICATION

The proposed system may support:

- organization-level fleet registry
- shared driver and resource availability
- assignment ownership
- role-separated operations
- common incident and maintenance history

### LIMITATION

Software cannot solve financing, governance conflict, consolidation policy, labor relations, or operator participation.

---

## 4.6 Mixed manual and automated monitoring creates provenance and verification requirements

### CONSTRAINT

Operational information may come from multiple source types. A human report and GPS-derived observation should not be represented as if they have the same provenance.

### EVIDENCE

PIA reported that the April 2026 Service Contracting Program used two monitoring systems: GPS tracking and manual monitoring by government agencies.

In an April 14, 2026 Malacañang press briefing, the acting Transportation Secretary stated that earlier manual monitoring made verification and validation difficult and that the current program emphasized GPS, while still allowing manual monitoring where GPS signal was erratic.

### SOURCES

**Organization:** Philippine Information Agency  
**Title:** *Service Contracting Program? Here’s what you need to know*  
**Publication date:** 2026-04-15  
**Direct URL:** https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/  
**Evidence classification:** CURRENT DIRECT / PROGRAM

**Organization:** Presidential Communications Office  
**Title:** *Malacañang Press Briefing ... DOE, DOTr, LTFRB, LANDBANK and DOF*  
**Publication date:** 2026-04-14  
**Direct URL:** https://pco.gov.ph/press-briefing/malacanang-press-briefing-of-palace-press-officer-and-pco-undersecretary-claire-castro-with-department-of-energy-doe-secretary-sharon-garin-department-of-transportation-dotr-acting-secretary-giov/  
**Evidence classification:** CURRENT DIRECT / OFFICIAL TRANSCRIPT

### KEYWORDS

Philippine Information Agency:
`Two monitoring systems are in place`
`GPS tracking on participating units`
`Manual monitoring by LTFRB`

Presidential Communications Office:
`verification and validation becomes very difficult`
`erratic iyong signal ng ating GPS`

### INTERPRETATION

The proposed system should preserve provenance. The system must know whether a status came from:

- manual entry by operations staff
- administrator correction
- imported record
- system-derived rule
- future telemetry integration

### SYSTEM IMPLICATION

Suggested fields:

- `source_type`
- `reported_by`
- `reported_at`
- `effective_at`
- `verification_status`
- `verified_by`
- `verified_at`
- `reason`
- `related_event_id`

Do **not** invent a numeric confidence score unless a defensible method exists.

### LIMITATION

Audit and provenance improve traceability. However, it does not make a manual report physically true.

---

## 4.7 Roadworthiness constrains usable fleet capacity

### CONSTRAINT

A registered fleet unit may be unavailable for service because it fails inspection, is under maintenance, or is otherwise not operationally ready.

### EVIDENCE

On February 4, 2026, PIA reported that LTFRB intensified roadworthiness inspections and checked tires, lights, seats, and other vehicle parts/accessories.

A February 23, 2026 PIA report on Batangas terminal inspections recorded citations including worn tires and missing fire extinguishers.

### SOURCES

**Organization:** Philippine Information Agency
**Title:** *LTFRB intensifies roadworthiness campaign on PUVs*  
**Publication date:** 2026-02-04  
**Direct URL:** https://pia.gov.ph/news/ltfrb-intensifies-roadworthiness-campaign-on-puvs/  
**Evidence classification:** CURRENT DIRECT / REGULATORY-OPERATIONAL

**Organization:** Philippine Information Agency  
**Title:** *Authorities inspect Batangas terminals, ports for safety compliance*  
**Publication date:** 2026-02-23  
**Direct URL:** https://pia.gov.ph/news/authorities-inspect-batangas-terminals-ports-for-safety-compliance/  
**Evidence classification:** CURRENT DIRECT / CASE-SPECIFIC

### KEYWORDS

Philippine Information Agency (roadworthiness campaign):
`checked the condition of tires, lights, seats`

Philippine Information Agency (inspection of batangas terminals):
`thin or worn-out tires`
`absence of fire extinguishers`

### INTERPRETATION

Fleet availability must be an operational state separate from vehicle identity and registration.

### SYSTEM IMPLICATION

- `AVAILABLE`
- `MAINTENANCE`
- `OUT_OF_SERVICE`
- optional `INSPECTION_HOLD`

Maintenance and inspection state should block new assignments when configured as unavailable.

### LIMITATION

The proposed system does not perform a mechanical inspection and should not label a vehicle "roadworthy" solely because a staff member changed a database status.

---

## 4.8 Driver and staff records require privacy/security controls

### CONSTRAINT

The proposed system stores employee and user identity and may store driver's-license information. These records are personal data; government-issued licenses are explicitly included in the law's definition of sensitive personal information.

### EVIDENCE

Republic Act No. 10173 applies to personal-information processing and requires reasonable and appropriate organizational, physical, and technical security measures.

### SOURCE

**Organization:** Republic of the Philippines, Official Gazette, and National Privacy Commission  
**Title:** *Republic Act No. 10173 - Data Privacy Act of 2012*  
**Date:** 2012-08-15  
**Official Gazette:** https://officialgazette.gov.ph/2012/08/15/republic-act-no-10173/  
**NPC copy:** https://privacy.gov.ph/data-privacy-act/  
**IRR:** https://privacy.gov.ph/implementing-rules-regulations-data-privacy-act-2012/  
**Evidence classification:** POLICY / LEGAL

### KEYWORDS

- `licenses or its denials, suspension or revocation`
- `reasonable and appropriate organizational, physical and technical measures`

### INTERPRETATION

The security argument for the proposed system should be based on actual data handled, not generic cybersecurity language.

## 4.9 SYSTEM IMPLICATION

- least-privilege RBAC
- authenticated staff accounts
- password hashing
- audit of privileged changes
- avoid placing license numbers/passwords in audit snapshots
- secure transport (HTTPS)
- restricted database access
- retention policy
- access logging for sensitive records
- documented lawful purpose for stored personal data

Furthermore, implementing these controls does not by itself certify legal compliance. A real deployment requires organizational policy, lawful-basis analysis, privacy notices, retention rules, and governance beyond application code.

## 4.10 IDENTIFIED CONSTRAINTS MATRIX TABLE

| Constraint | Evidence strength | Operational importance | Software addressability | Implementation feasibility | Priority |
|---|---|---:|---|---|---|
| Franchised fleet count may differ from actual operating supply | High, case-specific | High | Software can assist | High | Main |
| Service supply changes and cancelled runs | High, event-specific | High | Software can assist | High | Main |
| Route definitions change | Very high | High | Software-addressable | High | Main |
| Dispatch as an operational workflow | High | High | Software-addressable | High | Main |
| Consolidation and shared fleet management | Moderate to high, contextual | Medium | Software can assist | High | Supporting |
| Mixed manual and GPS monitoring | Very high | High | Software-addressable for provenance | High | Main |
| Roadworthiness affects availability | Very high | High | Software can assist | High | Main |
| Data privacy and security | Very high | High | Software can assist, with organizational governance | High | Main |

## 4.11 CONSTRAINT TO SYSTEM REQUIREMENT MATRIX TABLE

The following matrix translates the documented operational constraints into system requirements. The requirement column identifies what the application must represent, preserve, or enforce, while the system-capability column describes the corresponding software response.

| Constraint                                                                           | Operational consequence                                                                                | Requirement                                                                                            | System capability                                                                   |
| ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| A franchise or fleet master record does not establish current operating availability | Administrative records can overstate the number of units currently usable for service                  | Separate vehicle identity, operational availability, and active assignment                             | Vehicle availability state and history                                              |
| Planned service may change because of external operating conditions                  | Planned service may no longer reflect actual service                                                   | Preserve planned service and subsequent operational changes                                            | Trip and dispatch lifecycle with cancellation, curtailment, and change events       |
| Route modifications can make historical records difficult to interpret               | Earlier assignments may reference superseded route data                                                | Preserve route versions and effective periods                                                          | Effective-dated route versioning                                                    |
| Dispatch requires active coordination of vehicles, drivers, and operational changes  | A static schedule does not capture assigned resources, responsible actors, or subsequent state changes | Represent dispatch as a controlled assignment workflow                                                 | Dispatch board and controlled state transitions                                     |
| Shared fleet organizations coordinate multiple resources                             | Resource information may be distributed across personnel or records                                    | Maintain a common operational view                                                                     | Integrated fleet, driver, route, and role-controlled operational modules            |
| Operational information can come from different source types                         | Sources may conflict or differ in verification status                                                  | Preserve provenance and verification context                                                           | Source metadata, verification state, and traceability fields                        |
| Roadworthiness and maintenance can make a vehicle unavailable                        | A vehicle can remain a valid fleet record while being ineligible for dispatch                          | Track operational availability independently from vehicle identity and validate assignment eligibility | Maintenance and inspection holds integrated with dispatch validation                |
| Driver and staff information includes personal or sensitive data                     | Unauthorized access or disclosure creates privacy and security risk                                    | Apply least privilege and appropriate safeguards                                                       | RBAC, auditing, secure transport, restricted database access, and data minimization |

---
## 4.12 PHILIPPINE REGULATORY AND POLICY REQUIREMENTS

### a. Republic Act No. 10173, Data Privacy Act of 2012.

Under Republic Act No. 10173, personal-information processing must follow the principles of transparency, legitimate purpose, and proportionality and must be conducted fairly and lawfully (**Sec. 11**). Data collected should be relevant, adequate, and not excessive for its stated purpose (**Sec. 11(c)–(d)**), and identifiable personal information should be retained only for as long as necessary, subject to lawful exceptions (**Sec. 11(e)–(f)**). Personal information controllers must implement reasonable and appropriate organizational, physical, and technical security measures (**Sec. 20(a)–(c)**). Government-issued license information falls within the Act's definition of sensitive personal information (**Sec. 3(l)(3)**), so its processing is subject to the stricter conditions provided under **Sec. 13**.

| Data-protection requirement                                                                                       | Exact provision                                                                                                                                                                            | Basis in RA 10173                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Processing must have a legitimate purpose and must be lawful.                                                     | **Sec. 11 - General Data Privacy Principles**, especially **Sec. 11(a)–(b)**; for ordinary personal information, also **Sec. 12 - Criteria for Lawful Processing of Personal Information** | Sec. 11 requires adherence to transparency, legitimate purpose, and proportionality. Sec. 11(a) requires collection for specified and legitimate purposes, while Sec. 11(b) requires personal information to be processed fairly and lawfully. Sec. 12 provides the conditions under which processing of personal information is permitted.                                                                                             |
| Personal data should be relevant and not excessive for the stated purpose.                                        | **Sec. 11(c)–(d)**                                                                                                                                                                         | Sec. 11(c) requires personal information to be accurate and relevant to the purpose for which it is used. Sec. 11(d) requires the information collected and processed to be adequate and not excessive in relation to that purpose.                                                                                                                                                                                                     |
| Retention should be limited to what is necessary.                                                                 | **Sec. 11(e)–(f)**                                                                                                                                                                         | Sec. 11(e) limits retention to the period necessary for the purpose for which the data was obtained, legitimate business purposes, legal claims, or another period provided by law. Sec. 11(f) further requires identifiable personal information to be kept no longer than necessary for the purposes for which it was collected and processed, subject to the exceptions stated in the Act.                                           |
| Reasonable organizational, physical, and technical safeguards are required.                                       | **Sec. 20(a)–(c) - Security of Personal Information**                                                                                                                                      | Sec. 20(a) requires reasonable and appropriate organizational, physical, and technical measures to protect personal information. Sec. 20(b) requires protection against natural and human dangers, while Sec. 20(c) requires the level of security to account for the nature of the information, processing risks, organizational size and complexity, current data-privacy practices, and implementation cost.                         |
| Government-issued license information is sensitive personal information and therefore requires stricter handling. | **Sec. 3(l)(3) - Definition of Sensitive Personal Information**; processing is governed by **Sec. 13 - Sensitive Personal Information and Privileged Information**                         | Sec. 3(l)(3) includes information issued by government agencies that is peculiar to an individual, including licenses and their denial, suspension, or revocation, within the definition of sensitive personal information. A driver's license issued by the LTO therefore falls within this category. Sec. 13 generally prohibits processing of sensitive personal information unless one of the exceptions in Sec. 13(a)–(f) applies. |
### Relevant sections:

- **Sec. 3(l)(3)** - Government-issued information peculiar to an individual, including licenses, is classified as sensitive personal information.
- **Sec. 11(a)** - Personal information must be collected for specified and legitimate purposes.
- **Sec. 11(b)** - Personal information must be processed fairly and lawfully.
- **Sec. 11(c)** - Personal information must be accurate and relevant to its intended use.
- **Sec. 11(d)** - Personal information must be adequate and not excessive in relation to the purpose of collection and processing.
- **Sec. 11(e)–(f)** - Personal information must not be retained in identifiable form longer than necessary, subject to the exceptions provided by law.
- **Sec. 12(a)–(f)** - Provides the lawful grounds for processing ordinary personal information.
- **Sec. 13(a)–(f)** - Provides the limited circumstances in which sensitive personal information may be processed.
- **Sec. 20(a)–(c)** - Requires reasonable and appropriate organizational, physical, and technical security measures.

**Source:** Republic Act No. 10173, _Data Privacy Act of 2012_  
National Privacy Commission: [https://privacy.gov.ph/data-privacy-act/](https://privacy.gov.ph/data-privacy-act/)

### b. Relevant Policies and Operational Contexts

These sources do not automatically create software compliance requirements for Arc Transit. They provide Philippine public-transport policy and operational context that can justify particular domain-model and system-design decisions.
### 1. Route rationalization and route-plan changes

The LTFRB rules implementing the Public Utility Vehicle Modernization Program (PUVMP) state that routes and Certificates of Public Convenience (CPCs) granted during initial implementation are subject to the results of the DOTr route-rationalization study or the applicable Local Public Transport Route Plan (LPTRP). Additionally, more recent government statements also describe route rationalization/LPTRP work as determining public-transport routes and the appropriate number of units.

The proposed system does not model a route as a fixed immutable record. Route definitions, authorized coverage, and unit allocations may need to be updated when an approved route plan or regulatory decision changes. Historical route versions should be retained when operational records depend on an earlier route record.

**References:**
- LTFRB Memorandum Circular No. 2018-013, reproduced by the Supreme Court E-Library:
  https://elibrary.judiciary.gov.ph/thebookshelf/showdocs/10/90506
- Philippine Information Agency / Presidential Communications Office, "Transport agencies, LGUs rationalizing routes for PUVMP" (September 24, 2024):
  https://pia.gov.ph/press-release/transport-agencies-lgus-rationalizing-routes-for-puvmp/
### 2. Vehicle roadworthiness and operational availability

DOTr and LTO treat motor-vehicle inspection as a means of checking safety and roadworthiness. Government guidance states that vehicles should be inspected for safety and roadworthiness before registration renewal. Current LTFRB Land-Based Service Contracting Program terms likewise require participating vehicles to have valid and current LTO registration and to be maintained in a safe and roadworthy condition.

The proposed system distinguishes vehicle identity, operational availability, and assignment eligibility. A vehicle can remain part of the fleet registry while being unavailable because of maintenance or inspection status. Regulatory conditions such as current registration should be treated as separate eligibility checks rather than as physical availability states.

A dispatch-eligibility result may therefore be derived from multiple conditions, for example: the vehicle is operationally `AVAILABLE`, required registration information is current, no inspection hold is active, and other applicable assignment rules pass.

**References:**
- Philippine Information Agency / DOTr, "Motor vehicle inspection is mandatory before renewal of registration" (July 13, 2021):
  https://mirror.pia.gov.ph/press-releases/2021/07/13/motor-vehicle-inspection-is-mandatory-before-renewal-of-registration
- LTFRB Land-Based Service Contracting Program - Terms and Conditions:
  https://ltfrb-scp.ph/terms
### 3. Service Contracting Program: trip, route, and monitoring evidence

The current LTFRB Land-Based Service Contracting Program (LBSCP) terms state that payouts are computed from the receipt of an MMF or GPS Trip Report, as applicable. Only completed trips are eligible for payment. The terms also require operators to remain within the approved operational scope, including authorized routes or areas, unit trips, operating hours, and dispatch/headway commitments. The 2026 government rollout of the Service Contracting Program was also publicly described as using GPS tracking together with manual monitoring by transport agencies.

The proposed system can reasonably preserve evidence associated with a trip rather than storing only the final trip status. Useful records may include the assigned route, unit, driver, scheduled and actual trip times, completion status, GPS-derived evidence when available, monitoring or verification records, and any detected route deviation. These records support later verification of whether a trip was actually completed under its assigned operating conditions.

**References:**
- LTFRB Land-Based Service Contracting Program - Terms and Conditions:
  https://ltfrb-scp.ph/terms
- Philippine Information Agency, "Service Contracting Program? Here's what you need to know" (April 15, 2026):
  https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/
### 4. MMDA Bus Management and Dispatch System (BMDS)

The Metropolitan Manila Development Authority's Bus Management and Dispatch System (BMDS) has been used for the EDSA Busway. A 2023 Philippine Information Agency report states that the system supports systematic monitoring of the number of buses operating on the Busway and driver monitoring. Dispatching officers at PITX and Monumento were described as scanning drivers' unique QR codes to access driver information, including unsettled violations.

BMDS provides a Philippine operational reference example of dispatching being connected with identifiable drivers, authorized units, monitoring records, and enforcement-related information. For Arc Transit, this supports separating driver identity, vehicle assignment, dispatch events, and monitoring history instead of treating dispatch as only a simple vehicle-status change.

**Reference:**
- Philippine Information Agency, "MMDA's Bus Management and Dispatch System now monitors EDSA Busway" (June 19, 2023):
  https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway

### c. GTFS and GTFS-Realtime compatibility

If Arc Transit later needs to publish passenger-facing transit information or exchange transit data with external applications, it should consider using the General Transit Feed Specification (GTFS) instead of creating a proprietary feed format.

GTFS is an open standard for public-transit passenger information. It has two main parts:

- **GTFS Schedule** - describes relatively static transit information such as routes, stops, schedules, fares, and related geographic/service data.
- **GTFS Realtime** - works with an existing GTFS Schedule feed to provide up-to-date information about service operation.

**Official GTFS overview:**  
https://gtfs.org/documentation/overview/

**Official "What is GTFS?" page:**  
https://gtfs.org/getting-started/what-is-gtfs/

**GTFS Realtime reference:**  
https://gtfs.org/documentation/realtime/reference/

### GTFS Realtime information types relevant to Arc Transit

According to the official GTFS documentation, GTFS Realtime currently supports:

- **Trip updates** - delays, cancellations, and changed routes
- **Service alerts** - service disruptions such as moved stops or unforeseen events affecting a stop, route, or network
- **Vehicle positions** - information about vehicles, including their location and congestion level

For Arc Transit, these concepts could map to future features such as:

- publishing predicted or updated trip arrival/departure information;
- exposing trip delays or cancellations;
- publishing temporary service or route-disruption alerts;
- publishing vehicle locations to passenger-facing or third-party applications.

### Design scope

GTFS compatibility should remain an **optional or future integration** rather than a core requirement of the current proposed system.

The current system does not need to publish passenger-information feeds in order to perform its internal fleet, driver, route, dispatch, and trip, and operational-record functions. GTFS becomes relevant if Arc Transit later needs interoperability with trip-planning applications, public passenger-information systems, or other software that consumes standardized transit data.

GTFS should therefore be treated as an **external interoperability/output format**, not as the internal data model of Arc Transit.

### d. Important Keywords

→ `GTFS Schedule`  
→ `GTFS Realtime`  
→ `Trip updates - delays, cancellations, changed routes`  
→ `Service alerts`  
→ `Vehicle positions`  
→ `Passenger information`  
→ `Transit-data interoperability`

Furthermore, route or service disruptions may also be communicated through Service Alerts, and the GTFS Realtime reference contains more detailed mechanisms for modified trips and detours. Some of those mechanisms are marked experimental in the current specification, so they should not be treated as mandatory Arc Transit functionality.

GTFS Realtime is defined in relation to an existing GTFS Schedule feed. Therefore, Arc Transit should not implement GTFS Realtime alone without also having a corresponding GTFS Schedule dataset if interoperability is eventually added.

---

## 4.13 EXISTING PRODUCTION SYSTEMS

## 1. Fleetbase

**Organization:** Fleetbase
**Orientation:** Open-source logistics/supply-chain operating system  
**Target users:** Teams moving goods, people, or equipment  
**Data sources:** Manual workflows, APIs, driver app, live tracking, telematics integrations  
**Infrastructure assumption:** Self-hosted or cloud; broader logistics model

### Verified capabilities

- fleet management and dispatch
- vehicles/drivers/orders
- live tracking
- configurable workflows
- maintenance
- REST API/webhooks
- self-hosting

**Sources:**

https://github.com/fleetbase/fleetbase  
https://github.com/fleetbase/fleetops  
https://fleetbase.io/platform

### KEYWORDS

`open-source, modular operating system for logistics and supply chain operations`
`fleet management and dispatch`

### Project relevance

Fleetbase is a useful architecture and product reference. Its primary domain is broader logistics and order-execution workflows rather than fixed-route public-bus operational-state reconciliation.

### Boundary

Self-hosting and modularity should not be presented as unique differentiators because Fleetbase already demonstrates those characteristics.

## 2. Samsara

**Organization:** Samsara  
**Orientation:** Commercial connected-fleet and telematics platform  
**Target users:** Organizations operating connected vehicles and equipment  
**Data sources:** Vehicle Gateway, GPS, diagnostics, sensors, driver and mobile workflows  
**Infrastructure assumption:** Connected hardware and cloud platform

### Verified capabilities

- real-time GPS tracking
- vehicle diagnostics
- routing and dispatch
- commercial navigation
- workforce workflows
- safety and compliance functions
- fuel and energy functions

**Sources:**

https://www.samsara.com/products/telematics  
https://www.samsara.com/products/models/vehicle-gateway

### KEYWORDS

`Monitor your fleet with real-time GPS tracking`
`Routing & dispatch`
`captures real-time vehicle and driver data`

### Relevance to the Project

Samsara demonstrates that the proposed system should **not** compete on telemetry. The defensible distinction is a manual-first application-state workflow when vehicle gateway data is unavailable or outside project scope.

### Boundary

Public documentation does not prove Samsara lacks internal consistency or manual workflows. However, the proposed system does not claim it has features that Samsara cannot provide unless directly verified.

## 3. Geotab

**Organization:** Geotab  
**Orientation:** Commercial fleet telematics / connected transportation  
**Target users:** Commercial fleets; current APAC page explicitly includes the Philippines  
**Data sources:** GPS and vehicle tracking, engine data, driver behavior, integrations  
**Infrastructure assumption:** Connected telematics ecosystem

### Verified capabilities

- real-time vehicle/asset tracking
- driver-safety coaching
- predictive maintenance
- fuel monitoring
- routing/dispatch-related tools
- APIs, SDKs, and integration ecosystem

**Source:** https://www.geotab.com/apac/

### KEYWORDS

`track vehicles and assets in real time`
`Go from reactive to predictive maintenance`
`Philippines`

### Relevance to the Project

Geotab is a relevant regional technology comparator because its APAC material explicitly includes the Philippines. Comparative cost claims should not be made without Philippine-specific procurement evidence or quotations.

### Boundary

Its public APAC page does not establish all implementation/pricing conditions for a Philippine bus operator.

## 4. Trapeze

**Organization:** Trapeze Group  
**Orientation:** Public-transit workforce and operations management  
**Target users:** Transit agencies and operators  
**Infrastructure assumption:** Enterprise transit environment with configured workforce and operations data

### Verified capabilities

- bidding
- dispatching
- timekeeping
- workforce management
- operator sign-in through documented OPS modules

**Official source:** https://collaborate2.trapezegroup.com/tag/ops/

### KEYWORDS

`OPS streamlines and automates day-to-day tasks for bidding, dispatching, timekeeping, and workforce management`

### Relevance to the Project

The documented OPS capabilities confirm that dispatch, timekeeping, and workforce management are established functions in enterprise public-transport software.

### Boundary

The proposed system does not present a small dispatch board as a replacement for a full transit workforce and operations suite.

## 5. Optibus

**Organization:** Optibus  
**Orientation:** Public-transport planning, scheduling, operations, passenger information  
**Target users:** Public-transport agencies/operators  
**Infrastructure assumption:** Cloud-native operations platform with significant planning and operations data

### Verified capabilities

- planning
- crew and vehicle scheduling
- operations management
- allocation and dispatch
- payroll and timekeeping
- optimization
- real-time operational adjustments

**Sources:**

https://optibus.com/product/  
https://optibus.com/product/operations/  
https://optibus.com/product/scheduling/

### KEYWORDS

`end-to-end operating system for public transportation`

`Manage allocation, dispatch, payroll`

### Relevance to the Project

Optibus demonstrates that planning, scheduling, optimization, and operations management are already established capabilities in mature public-transport software.

### Boundary

The public product material does not establish that every manual-first workflow introduced by the proposed system is absent.

## 6. INIT

**Organization:** INIT  
**Orientation:** Integrated public-transport IT and intelligent transportation systems  
**Target users:** Public-transport providers  
**Infrastructure assumption:** Integrated planning, dispatch, operations control, real-time passenger information

### Verified capabilities

- planning and dispatching
- operations control
- real-time passenger information
- disruption management
- integrated data flow

**Sources:**

https://www.initse.com/ende/home/  
https://www.initse.com/enus/news-resources/knowledge-database/2025/ghost-buses-when-transit-data-haunts-your-riders-1/

### KEYWORDS

`Planning & Dispatching`
`trip cancellations, curtailments, diversions, and schedule adjustments`
`executed in the field but never modified in the central system`

### Relevance to the Project

INIT's "ghost bus" discussion provides a useful international architectural reference for the proposed system's reconciliation concept.

### Boundary

This is **international industry evidence**, not proof that the same central-system disconnect exists across Philippine bus operators.

## 7. GIRO / HASTUS

**Organization:** GIRO  
**Orientation:** Transit scheduling and resource optimization  
**Target users:** Transit agencies/operators  
**Infrastructure assumption:** Formal timetable, vehicle and crew scheduling environment

### Verified capabilities

- timetable creation
- vehicle schedules
- what-if scenarios
- vehicle blocks
- capacity, restrictions, and unit availability
- crew scheduling

**Source:** https://www.giro.ca/en-us/our-solutions/hastus-software/hastus-for-schedulers/

### KEYWORDS

`Create timetables and vehicle schedules`
`unit availability`

### Project relevance

The proposed system does not implement HASTUS-class mathematical optimization.

### Boundary

The proposed system may store schedules and assignments, but these should be described as operational records rather than as an optimization engine.

## 8. CleverCAD

**Organization:** Hitachi Mobility Multimodal Solutions  
**Orientation:** Public-transit CAD/AVL operations control  
**Target users:** Transit agencies/operators  
**Data sources:** Real-time vehicle location and status, communications, and operational events  
**Infrastructure assumption:** CAD/AVL with connected fleet and control infrastructure

### Verified capabilities

- real-time vehicle location and status
- dispatch communications
- event-driven incident management
- disruption management
- passenger update workflows

**Source:** https://www.hitachimms.com/products/clevercad/

### KEYWORDS

`real-time picture of the location and status`
`Event-Driven Incident Management`
`Disruption Management`

### Project relevance

CleverCAD demonstrates that enterprise incident and disruption management already exists at the CAD/AVL level.

### Boundary

The proposed system is not an alternative CAD/AVL stack.

## 9. Seoul TOPIS BMS and BIS

**Organization:** Seoul Metropolitan Government / TOPIS  
**Orientation:** Government integrated transport management and bus information  
**Target users:** Public-sector transport control and public information  
**Data sources:** Real-time bus location and broader integrated traffic data  
**Infrastructure assumption:** City-scale data infrastructure

### Verified capabilities

- BMS and BIS
- real-time bus location processing
- bus-operation management
- punctuality support
- integrated transport information

**Source:** https://topis.seoul.go.kr/openEngBms.do

### KEYWORDS

`collects, processes, and analyzes bus location information in real-time`

### Project relevance

TOPIS is the telemetry-rich/city-scale comparison that the proposed system intentionally does not reproduce.

### Boundary

Seoul's transportation infrastructure is an international architectural reference and should not be treated as evidence of Philippine operating conditions.

## 4.14 COMPARISON MATRIX OF FEATURE CAPABILITIES

The matrix below compares only capabilities that can be reasonably mapped across the reviewed systems. Because the products occupy different layers of the transport-technology ecosystem, the table should be interpreted as a functional comparison rather than as a product ranking. A `NOT ESTABLISHED` entry means that the reviewed official documentation did not establish the capability in the form being compared; it does not mean that the product lacks the capability.

| Capability                             | Samsara         | Geotab                    | Trapeze         | Optibus         | INIT            | HASTUS          | CleverCAD       | TOPIS           | Fleetbase       | Arc Transit     |
| -------------------------------------- | --------------- | ------------------------- | --------------- | --------------- | --------------- | --------------- | --------------- | --------------- | --------------- | --------------- |
| Fleet/vehicle records                  | YES             | YES                       | PARTIAL         | YES             | YES             | YES             | YES             | YES             | YES             | YES             |
| Manual dispatch assignment             | PARTIAL         | PARTIAL                   | YES             | YES             | YES             | PARTIAL         | YES             | NOT ESTABLISHED | YES             | YES             |
| GPS/AVL live location                  | YES             | YES                       | NOT ESTABLISHED | PARTIAL         | YES             | NOT ESTABLISHED | YES             | YES             | YES             | OUT OF SCOPE    |
| Vehicle diagnostics                    | YES             | YES                       | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | PARTIAL         | OUT OF SCOPE    |
| Mathematical scheduling optimization   | PARTIAL         | PARTIAL                   | PARTIAL         | YES             | YES             | YES             | NOT ESTABLISHED | PARTIAL         | YES             | OUT OF SCOPE    |
| Incident and disruption workflow           | PARTIAL         | PARTIAL                   | NOT ESTABLISHED | PARTIAL         | YES             | NOT ESTABLISHED | YES             | PARTIAL         | PARTIAL         | YES             |
| Maintenance workflow                   | YES             | YES                       | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | YES             | YES             |
| Human-source provenance                | NOT ESTABLISHED | NOT ESTABLISHED           | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | PARTIAL         | YES             |
| Explicit app-level contradiction queue | NOT ESTABLISHED | NOT ESTABLISHED           | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | DESIGN PROPOSAL |
| Audit history                          | PARTIAL         | PARTIAL                   | PARTIAL         | PARTIAL         | PARTIAL         | PARTIAL         | PARTIAL         | PARTIAL         | YES             | YES             |
| Self-host option                       | NOT ESTABLISHED | NOT ESTABLISHED           | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED | YES             | YES             |
| Passenger information                  | PARTIAL         | NOT ESTABLISHED           | NOT ESTABLISHED | YES             | YES             | NOT ESTABLISHED | YES             | YES             | PARTIAL         | OUT OF SCOPE    |


**LEGEND**
- **YES** = official documentation establishes it.
- **PARTIAL** = official documentation establishes a related capability, but the exact workflow or scope differs from the proposed system.
- **NOT ESTABLISHED** = the reviewed official documentation does not establish the capability in the compared form. This is not equivalent to **NO**.
- **OUT OF SCOPE** = the capability is intentionally excluded from the proposed system.

The reviewed transport-technology ecosystem already provides substantial functionality across several layers of transit and fleet operations. Samsara and Geotab document connected-fleet telematics, GPS, diagnostics, and operational workflows. Trapeze documents workforce and dispatch functions. Optibus and HASTUS provide planning, scheduling, and optimization capabilities. INIT and CleverCAD demonstrate integrated transit operations control and disruption management. Seoul TOPIS provides a city-scale bus-management and information environment, while Fleetbase demonstrates an open-source platform for logistics, fleet management, and dispatch. In the Philippine context, the MMDA Bus Management and Dispatch System provides direct evidence of digital dispatch monitoring on the EDSA Busway.

These systems establish that the project gap cannot reasonably be defined as the absence of fleet management, dispatch, monitoring, maintenance integration, scheduling software, or self-hosted operational platforms. The project should instead be differentiated by its deliberately limited operating assumptions, its emphasis on human-source provenance, and its explicit application-level operational-state history and reconciliation workflow.

Accordingly, this project does not claim that:

- no existing system provides fleet management;
- no existing system provides dispatch;
- no existing system performs anomaly or consistency detection;
- no existing system integrates maintenance information;
- no existing system can be self-hosted; or
- Philippine public transport has no digital monitoring system.

---

# V. DEFENSIBLE PROJECT OPPORTUNITY

The identified constraints and the comparison with related systems support a narrow project opportunity. The proposed system can investigate how a small internal transit-operations application maintains a coherent operational record when staff-entered information remains an important source of operational state and when connected-fleet or enterprise transit-control infrastructure is not assumed.

The operating model distinguishes planned, assigned, reported, verified, current, and stale information. It preserves the source and timing of operational changes, applies deterministic validation rules, identifies contradictions within the application's own records, and requires human review where software cannot independently establish physical truth or determine an authoritative resolution.

The defensible gap is therefore a difference in operating model, project scale, workflow emphasis, and infrastructure assumptions. It is not evidence that mature enterprise platforms are incapable of implementing similar controls. The research-to-system connection can be illustrated as:

**human or external source -> timestamped operational event -> validated state transition -> current-state projection -> exception or reconciliation rule -> human resolution -> audit record**

---

# VI. PROPOSED SYSTEM POSITION

The proposed system, Arc Transit, is a transit-operations coordination and operational-state reconciliation platform intended for the internal operations of a bus operator or cooperative. It provides structured management of fleet units, drivers, effective-dated routes, trip and dispatch assignments, incidents, maintenance activity, and manually reported operational changes. The system emphasizes explicit state models, provenance, history, controlled workflow transitions, role-based access, and rule-based detection of inconsistent or stale records.

The project is deliberately scoped as a small-scale browser-based application implemented as a modular monolith rather than as a replacement for advanced CAD/AVL, connected-fleet telematics, city-scale transport management, or mathematical scheduling systems. Its purpose is to coordinate and preserve operational records when human-entered information remains an important source of operational state. The application does not independently determine vehicle location, mechanical condition, passenger load, or physical truth unless an appropriate external data source is integrated.

---

# VII. SYSTEM FEATURES

The proposed system features are derived from the operational constraints identified in the Philippine evidence and from the boundaries established through the comparison with existing transport systems. Each capability defines a specific software responsibility, the information required by the workflow, the expected system behavior, and the limits of what the application can determine independently.
## 7.1 Fleet Identity and Operational Availability

The proposed system should represent vehicle identity separately from current operational availability. A vehicle may remain part of the registered or managed fleet while being temporarily unavailable because of maintenance, inspection, operational suspension, or another documented condition.

**Requirement:** Maintain separate records for vehicle identity and operational availability.
**Primary user roles:** System Administrator and Operations Staff

**Required information:**

- fleet-unit identity and reference information
- current availability state
- effective date and time of the availability state
- reason for the state change
- source of the information

**System outputs:**

- current operational availability
- historical availability records
- assignment eligibility result

**Potential failure conditions:**

- an unavailable vehicle receives a new dispatch assignment
- the most recent availability information has become stale
- the recorded availability conflicts with an active maintenance record

**Audit requirement:** Record the responsible user, timestamp, affected vehicle, previous state, new state, and reason for the change where applicable.

**Limitation:** The application cannot independently determine whether a vehicle is physically roadworthy or actually operating unless an inspection, telemetry source, or other reliable external verification mechanism is integrated.
## 7.2 Driver Availability and Qualification Records

The proposed system should maintain driver information required for operational assignment while separating administrative information from current duty availability. Driver qualification information must also be protected because government-issued license information may constitute sensitive personal information under Philippine law.

**Requirement:** Maintain driver identity, qualification, and current duty availability for use in dispatch validation.

**Required information:**

- driver identifier
- current duty availability
- license number and classification where operationally necessary
- license expiry date
- employment or administrative status where applicable

**Potential failure conditions:**

- assignment of a driver with an expired license
- assignment of a driver currently recorded as unavailable
- overlapping assignments involving the same driver

**Audit requirement:** Record controlled changes to driver qualification and availability information.

**Limitation:** Information stored in the application does not independently verify the current validity of a government-issued license unless a reliable external verification source is integrated.
## 7.3 Route and Route-Version Management

The proposed system should preserve historical route definitions instead of overwriting them whenever a route is modified. This allows past trips and dispatch assignments to remain associated with the route definition that was valid at the time of operation.

**Requirement:** Maintain effective-dated route definitions and preserve historical route versions.

**Required information:**

- route identity
- route version
- configured stops
- effective start date
- effective end date where applicable
- operational status

**System outputs:**

- current route definition
- historical route versions
- route version applicable to a particular service date

**Potential failure conditions:**

- an assignment references a route version outside its effective period
- historical route information is overwritten
- route changes create ambiguity in earlier operational records

**Audit requirement:** Record creation and modification of route versions, including the responsible user and effective period.

**Limitation:** The proposed system can represent route information but does not authorize, approve, or replace the regulatory processes of the Land Transportation Franchising and Regulatory Board.
## 7.4 Trip and Dispatch Assignment Lifecycle

The dispatch feature should be represented as a controlled operational workflow rather than as a simple schedule field. A dispatch assignment connects a specific service operation with a vehicle, driver, route, service date, planned time, and responsible staff action.

**Requirement:** Maintain a structured lifecycle for trip planning and dispatch assignment.

**Required information:**

- service date
- applicable route version
- assigned vehicle
- assigned driver
- planned start and end times
- assignment status
- responsible dispatcher or staff account

**System outputs:**

- current assignment state
- historical assignment events
- vehicle and driver eligibility results
- resource-conflict results

**Potential failure conditions:**

- double booking of a vehicle
- double booking of a driver
- assignment of an unavailable vehicle or driver
- assignment using an invalid route version
- invalid transition between assignment states

**Audit requirement:** Record every state-changing assignment operation together with the responsible user, timestamp, previous state, and resulting state.

**Limitation:** The application cannot independently confirm that a vehicle physically departed, reached the route, or completed service unless an external operational data source is integrated.
## 7.5 Manual Operational Reporting and Provenance

The proposed system should preserve the source and timing of manually reported operational information. A human-entered report should remain distinguishable from information obtained through imported records, system-derived rules, or future telemetry integration.

**Requirement:** Preserve source provenance, effective time, and verification status for manually reported operational information.

**Required information:**

- reported operational condition
- effective date and time
- reporting user
- source type
- supporting notes
- verification status where applicable

**System outputs:**

- append-only operational observation record
- current operational-state update where permitted
- verification status
- historical reporting trail

**Potential failure conditions:**

- stale operational report
- conflicting reports concerning the same vehicle or trip
- unverified information being treated as confirmed information
- missing source or effective timestamp

**Audit requirement:** Preserve the submission, later verification actions, corrections, and responsible users.

**Limitation:** Provenance and auditability establish who reported information and when it was recorded. They do not establish that the reported physical event actually occurred.
## 7.6 Incident Management Workflow

The proposed system should provide a structured method for recording operational incidents and linking them to the affected trip, vehicle, route, or assignment where applicable. Incident handling should preserve both the initial report and subsequent actions taken by operations staff.

**Requirement:** Maintain traceable incident records from initial reporting through resolution and closure.

`OPEN -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> CLOSED`

**Required information:**

- incident type
- severity
- affected operational record
- reported time
- description
- responsible staff member
- resolution information

**Limitation:** Incident detection remains dependent on staff reporting unless future sensors, vehicle systems, or external monitoring integrations are introduced.
## 7.7 Maintenance and Inspection Holds

The proposed system should connect maintenance activity with vehicle operational availability. A vehicle recorded as undergoing maintenance or subject to an inspection hold should not remain eligible for normal dispatch under the configured business rules.

**Requirement:** Maintenance and inspection status must influence vehicle assignment eligibility.

`REQUESTED -> SCHEDULED -> IN_PROGRESS -> COMPLETED`

A maintenance request may also transition from `REQUESTED` or `SCHEDULED` to `CANCELLED` when appropriate.

**System behavior:** While an active maintenance or inspection hold exists, the associated vehicle should be recorded as unavailable for new dispatch assignments unless an authorized correction or override is made.

**Audit requirement:** Record maintenance creation, state changes, responsible staff, timestamps, and completion information.

**Limitation:** The maintenance module records maintenance activity and operational availability. It does not perform engine diagnostics, condition monitoring, or predictive maintenance.
## 7.8 Reconciliation and Exception Management

The reconciliation capability should identify contradictions, stale conditions, and incomplete relationships within the application's own operational records. It is intended to support staff review when records that should logically agree no longer match.

Examples include:

- a vehicle recorded as unavailable while still assigned to an active trip
- an active maintenance record while the vehicle remains marked available
- overlapping vehicle assignments
- overlapping driver assignments
- a cancelled trip that remains active in the current-state projection
- an active trip with no update after its expected completion time
- an operational report that remains unverified when verification is required

**Requirement:** Detect predefined application-level inconsistencies and create an exception for human review.

**System output:** An exception record should identify the affected records, the violated rule, the time the exception was detected, its current resolution status, and any corrective action taken.

**Limitation:** Reconciliation is deterministic consistency checking based on configured rules. It is not artificial intelligence, machine learning, or independent verification of real-world events.
### a. Reconciliation and Exception rules

| Rule                                  | Inputs                                         | Contradiction / condition                                      | Effect         | System response                        | Human action                                     |
| ------------------------------------- | ---------------------------------------------- | -------------------------------------------------------------- | -------------- | -------------------------------------- | ------------------------------------------------ |
| Unavailable vehicle assigned          | vehicle availability and active assignment     | availability is not AVAILABLE while the assignment is active   | HIGH           | block new assignment; flag existing    | confirm availability or reassign                 |
| Maintenance and availability conflict | active maintenance and availability            | maintenance IN_PROGRESS while vehicle AVAILABLE                | HIGH           | exception                              | correct the state or close maintenance           |
| Expired driver license                | license expiry and service date                | license expiry precedes service date                           | HIGH           | block dispatch creation                | update the verified record or replace the driver |
| Unavailable driver assigned           | driver duty state and assignment               | UNAVAILABLE with active assignment                             | HIGH           | block or flag                          | reassign or correct state                        |
| Vehicle overlap                       | time window and assignment                     | same vehicle in overlapping non-cancelled trips                | HIGH           | reject transaction                     | revise assignment                                |
| Driver overlap                        | time window and assignment                     | same driver in overlapping non-cancelled trips                 | HIGH           | reject transaction                     | revise assignment                                |
| Invalid route version                 | service date and route effective period        | trip uses route version outside effective period               | Medium to high | reject or require override with reason | select valid version                             |
| Stale active trip                     | planned end and current time and no completion | trip remains IN_SERVICE beyond configurable grace period       | MEDIUM         | freshness exception                    | confirm, update, or correct                      |
| Cancelled trip still active           | cancellation event and active state            | cancellation exists while current projection remains active    | HIGH           | exception                              | reconcile projection                             |
| Unresolved incident too old           | incident status and age threshold              | OPEN or ACKNOWLEDGED beyond a configured operational threshold | MEDIUM         | aging alert                            | investigate and update                           |
| Unverified manual report              | report source and verification requirement     | report used for critical decision but still UNVERIFIED         | MEDIUM         | warning, not automatic rejection       | verify or document override                      |
| Missing causal link                   | replacement action and affected trip           | vehicle replaced but no reason/event reference                 | Low to medium  | completeness warning                   | attach an incident or reason                     |

## 7.9 Operational Home Dashboard

The operational dashboard should present a concise view of the current records maintained by the system. Its purpose is fleet operational status monitoring and descriptive analysis.

Appropriate dashboard indicators include:

- available fleet units
- currently active assignments
- reported delays and service disruptions
- open incidents
- vehicles under maintenance
- active inspection holds
- stale operational reports
- unverified reports
- unresolved reconciliation exceptions

The dashboard should clearly communicate that displayed values represent the most recent information known to the application. A value should not be described as real-time unless it is supported by a source that actually provides real-time data.

### a. Vehicle administrative lifecycle

| States   | Purpose                                         |
| -------- | ----------------------------------------------- |
| ACTIVE   | Managed fleet unit                              |
| INACTIVE | Temporarily not part of normal operational pool |
| ARCHIVED | Historical/non-current master record            |

### b. Vehicle availability

| State | Meaning |
|---|---|
| AVAILABLE | Eligible for assignment subject to other validations |
| MAINTENANCE | Unavailable due to active maintenance |
| INSPECTION_HOLD | Unavailable pending required inspection/clearance |
| OUT_OF_SERVICE | Explicitly unavailable |
| UNKNOWN | Current physical/operational availability not reliably known |

## c. Driver operational state

Driver administrative or employment status, duty state, and dispatch assignment should remain separate.

### d. Driver availability

| State | Meaning |
|---|---|
| AVAILABLE | Eligible for assignment subject to qualification and conflict checks |
| UNAVAILABLE | Not currently eligible for assignment |
| UNKNOWN | Current availability has not been established reliably |

### e. Driver duty state

A separate duty state may be used when the prototype needs to distinguish whether a driver is currently working:

`OFF_DUTY -> ON_DUTY -> OFF_DUTY`

Whether a driver is assigned to a trip should be determined from the dispatch assignment rather than stored as a driver-availability state.

### f. Dispatch and trip assignment

`PLANNED → ASSIGNED → DISPATCHED → IN_SERVICE → COMPLETED`

Alternative transitions:

- `PLANNED or ASSIGNED → CANCELLED`
- `DISPATCHED or IN_SERVICE → CURTAILED`
- route change event → `DIVERTED` or linked diversion event

### g. Incident

`OPEN → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → CLOSED`

### h. Maintenance

Primary lifecycle:

`REQUESTED -> SCHEDULED -> IN_PROGRESS -> COMPLETED`

Alternative transition:

`REQUESTED or SCHEDULED -> CANCELLED`

An approval state should be introduced only if the implemented workflow includes a distinct approval responsibility.

### i. Verification state

For manually reported operational observations:

`UNVERIFIED -> CONFIRMED`

A report may also become disputed:

`UNVERIFIED or CONFIRMED -> DISPUTED`

A disputed report should record a resolution outcome rather than ending at a generic state that loses the result. Example outcomes include:

- `CONFIRMED`
- `REJECTED`
- `SUPERSEDED`

The application should not automatically treat the latest report as verified truth.

### j. Freshness

Prefer **computed freshness** from `effective_at` and `reported_at` and a configured threshold:

- CURRENT
- AGING
- STALE

Do not store freshness as an arbitrary permanent fact if it can be derived from time.

---

# VIII. DATA MODELS

## 8.1 Core entities

- `organization` (single tenant, optional explicit record)
- `app_user`
- `role`
- `user_role`
- `fleet_unit`
- `vehicle_availability_current`
- `vehicle_availability_event`
- `driver`
- `driver_availability_current`
- `driver_availability_event`
- `driver_duty_state`
- `route`
- `route_version`
- `route_stop_version`
- `route_schedule`
- `trip_instance`
- `dispatch_assignment`
- `dispatch_event`
- `operational_report`
- `incident`
- `incident_event`
- `maintenance_record`
- `maintenance_event`
- `exception_case`
- `exception_event`
- `audit_event`

## 8.1 Current state and event history

The prototype should maintain efficient current-state records while also preserving append-only business-event history for causality, audit, and reconstruction. This design does not require full event sourcing.

State-changing transactions should update the relevant current-state record and append the corresponding historical event within the same database transaction. For example, a change to `vehicle_availability_current` should be accompanied by a new `vehicle_availability_event` that preserves the actor, source, effective time, previous state, new state, and reason where applicable.

The current-state record is used for operational queries and validation. The event-history record preserves how the current state was reached.

## 8.3 Provenance fields

For human-entered or multi-source operational data:

- `source_type`
- `source_reference`
- `reported_by_user_id`
- `reported_at`
- `effective_at`
- `verification_status`
- `verified_by_user_id`
- `verified_at`
- `reason_code`
- `notes`
- `related_event_id`

## 8.4 Audit fields

- `actor_user_id`
- `actor_username_snapshot`
- `action_type`
- `entity_type`
- `entity_id`
- `occurred_at`
- `correlation_id`
- `outcome`
- `previous_value_summary` where appropriate
- `new_value_summary` where appropriate

Password hashes, authentication secrets, and unnecessary sensitive personal information should not be copied into audit snapshots.

---

# IX. SECURITY AND PRIVACY REQUIREMENTS

Evidence basis: Republic Act No. 10173 and its implementing rules.

## 9.1 Required application controls for the prototype

- authenticated access
- role-based authorization
- least privilege
- secure password hashing
- CSRF/session protection appropriate to framework
- HTTPS in deployed environments
- database not directly exposed to the public internet
- server-side validation
- parameterized ORM/query usage
- audit security-sensitive and operational state-changing actions
- protect driver-license data and other sensitive personal information
- do not log secrets
- dependency and security scanning as QA, not proof of security
- backup/restore procedure
- retention/deletion design for personal data

## 9.2 User Roles

**SYSTEM_ADMIN**
- account/role administration
- configuration
- full operations access
- audit search

**OPERATIONS_STAFF**
- fleet, driver, and route viewing
- dispatch
- manual reports
- incidents
- maintenance
- dashboards

Within the prototype, `OPERATIONS_STAFF` performs operational functions that could be separated into dispatcher, maintenance, and supervisory responsibilities in a production deployment.

A production deployment may require explicit roles for dispatch personnel, maintenance personnel, supervisors, auditors, privacy or compliance reviewers, and other operational responsibilities.

## 9.3 What this does not prove

- legal compliance certification
- secure deployment configuration in every environment
- absence of vulnerabilities
- proper organization-wide privacy governance

---

# X. ARCHITECTURAL IMPLICATIONS

| Constraint                                | Architecture response                                                     | Reason                                                                  | Trade-offs                                             |
| ----------------------------------------- | ------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------ |
| Solo development                          | Modular monolith                                                          | Keeps deployment/test complexity low while preserving domain boundaries | Modules still share process/runtime                    |
| Strongly relational workflow              | PostgreSQL                                                                | Transactions, FK constraints, temporal/event relationships              | Requires schema discipline/migrations                  |
| Mutable operational state with traceable history | Current-state records and append-only business events                                 | Fast current view without losing history                                | More write logic and storage                           |
| Manual and multi-source reports               | Provenance metadata                                                       | Distinguishes report source and verification                            | Additional UI/data-entry burden                        |
| Cross-module consistency                  | Application-service rules and reconciliation worker                       | Centralizes validation and exception generation                         | Rules require careful transaction boundaries           |
| Privacy and security                          | Security measures including server-side sessions, RBAC, secure deployment | Fits browser-only prototype and least privilege                         | External clients later may require OAuth/OIDC/API auth |
| Route changes                             | Effective-dated route versions                                            | Preserves historical correctness                                        | More complex queries/UI                                |
| Future integration                        | REST/OpenAPI with optional future GTFS export                          | Avoids custom closed integration model                                  | Extra API surface to maintain                          |
| No need for distributed system            | No microservices or Kafka                                                    | Avoids unnecessary operational complexity                               | Less independent scaling                               |

## 10.1 Software Tools Used

- Java 21
- Spring Boot
- Spring Modulith
- Spring Security
- PostgreSQL
- Flyway
- Maven
- Docker Compose
- JUnit and Testcontainers
- Vaadin Flow
---

# XI. FEATURE PRIORITIES

Feature priority is based on the strength of the evidence-to-requirement chain, the importance of each capability to the proposed operating model, and whether the feature can be implemented and evaluated within the current prototype scope.

## 11.1 Main features

The main features establish the principal software contribution of the project: structured operational coordination, explicit state management, source traceability, validation, and application-level reconciliation. These capabilities should be implemented before secondary reporting or integration work because they directly support the documented constraints and the proposed evaluation plan.

- fleet master records and vehicle availability
- driver records and duty availability
- route and route-version management
- trip and dispatch assignment lifecycle
- incident workflow
- maintenance workflow
- manual operational reporting and provenance
- event and state history
- role-based access control
- audit logging
- dispatch and state-transition validation rules
- reconciliation and exception queue
- descriptive operational dashboard
- controlled scenario test harness

## 11.2 Supporting features

Supporting features improve daily usability, administrative efficiency, and interpretation of the operational record. They remain useful, but the research argument does not depend on them as strongly as it depends on the core workflows.

- static route map
- fleet, driver, route, and dispatch search and filtering
- CSV import and export
- exception aging and freshness indicators
- operational summary reports
- configurable reason codes

## 11.3 Optional features

Optional features provide useful extension points and may improve the quality of the prototype, but they are not required for the system to demonstrate its main research objectives.

- supervisor verification workflow
- supporting-document attachments
- simple external API
- GTFS Schedule export
- additional user roles
- email or notification integration

## 11.4 Future Development features

Future capabilities require real operational data, connected infrastructure, larger datasets, or additional organizational requirements that are not available within the current prototype. They should remain future integration points rather than implied present capabilities.

- GPS or AVL integration
- GTFS-Realtime publishing
- live telemetry
- automatic ETA
- passenger-information feed
- advanced business intelligence
- forecasting
- predictive maintenance
- mathematical scheduling optimization
- multi-tenant deployment
- offline synchronization and store-and-forward behavior

## 11.5 Out-of-scope features

The following capabilities are intentionally excluded because they do not follow from the current evidence, depend on unavailable data or infrastructure, or would expand the project beyond its defined research problem.

- autonomous dispatch
- AI chatbot
- LLM-based operations control
- machine-learning anomaly detection without a defensible dataset
- digital twin
- fare and payment processing
- city-scale traffic control
- regulatory enforcement platform
- unrelated distributed-ledger functionality

## 11.6 System Limitations

The proposed system cannot independently determine:

- physical vehicle location
- actual roadworthiness
- actual passenger count unless manually or externally supplied
- physical truth of a manual report
- telemetry-grade ETA
- engine fault codes
- driver behavior such as harsh braking or speeding
- whether an incident physically occurred as described
- optimal city-wide routes
- optimal crew or vehicle schedules
- regulatory compliance status unless a valid external process or data source is integrated

The proposed system cannot prove:

- fewer accidents
- less congestion
- lower fares
- better passenger satisfaction
- improved national/public-transit productivity
- cheaper total cost than commercial vendors
- better performance than commercial products such as Samsara, Geotab, or Optibus
- legal compliance merely because RBAC and audit logging exist

The evaluation can demonstrate only software-level behavior measured under the defined test conditions.

---

# XII. EVALUATION PLAN

The evaluation plan is limited to software-level outcomes that can be reproduced with controlled test data. The experiments use descriptive names rather than internal codes so that each test remains understandable when cited elsewhere in the report.

## 12.1 Invalid assignment prevention test

**Research question:** Can the proposed system prevent assignments that violate defined vehicle, driver, route, and overlap rules?

**Scenario:** Generate valid and invalid dispatch-assignment cases.

**Independent variable:** Whether the proposed assignment satisfies the defined business rules.

**Dependent variable:** Whether the assignment is accepted or rejected.

**Procedure:** Execute a fixed test set containing unavailable vehicles, expired driver licenses, overlapping vehicle assignments, overlapping driver assignments, route versions outside their effective periods, and valid control cases.

**Metrics:**

- invalid-assignment rejection rate
- valid-assignment acceptance rate
- false-positive rate
- false-negative rate

**Expected observable result:** Defined invalid cases are rejected while valid control cases are accepted.

**Limitation:** This evaluates configured software rules. It does not establish real-world transport safety.

## 12.2 Reconciliation detection test

**Research question:** Can the proposed system identify seeded contradictions between operational records?

**Scenario:** Inject known contradictions such as maintenance and availability mismatch, cancelled-but-active trip, unavailable resource assignment, and stale active trip.

**Procedure:** Run the reconciliation process against a controlled dataset with a known number of seeded exceptions and non-exception control cases.

**Metrics:**

- detection recall
- precision
- false-positive rate
- time to detection

**Expected observable result:** The system detects the seeded contradictions defined by the implemented rules while minimizing false positives among control cases.

**Limitation:** Synthetic scenarios validate the implemented rules but do not prove coverage of all production anomalies.

## 12.3 Recovery traceability test

**Research question:** Does the final operational record preserve the causal chain after a disruption?

**Scenario:** A vehicle assigned to a trip becomes unavailable, an incident or operational report is recorded, a replacement vehicle is assigned, and the trip is resolved.

**Metric:** Percentage of required causal links present in the final record:

`reported problem -> affected trip -> operational action -> replacement assignment -> resolution`

**Expected observable result:** The completed record preserves the expected links between the disruption, response, replacement, and resolution.

**Limitation:** The test evaluates record traceability, not passenger service quality.

## 12.4 Record completeness test

**Research question:** Does the structured workflow consistently capture the fields required by the proposed operational model?

**Scenario:** Use a fixed set of simulated operational cases and compare completed records against a predefined required-field checklist.

**Metric:** Record-completeness percentage.

A spreadsheet or minimal form may be used as a controlled comparison only when both workflows receive the same scenarios and required-field expectations. Such a comparison should not be generalized as evidence about actual Philippine operator performance.

## 12.5 Time-to-update test

**Research question:** After a staff user records an operational change, how long does the application take to reflect the new current state in the operational view?

**Metric:** Submission-to-visible-state latency in milliseconds.

**Limitation:** This measures application propagation after data entry. It does not measure the delay between a physical field event and the time a staff member reports it.

## 12.6 Audit completeness test

**Research question:** Are controlled state-changing operations fully traceable in the audit record?

**Metric:** Percentage of state-changing operations that contain the required actor, timestamp, entity, action, outcome, and previous and new state where applicable.

**Expected observable result:** Every tested state-changing operation produces the required audit information according to the audit specification.

**Limitation:** A complete audit event proves what the application recorded. It does not prove the physical truth of the underlying report.

## 12.7 Authorization and access-control test

**Research question:** Does each application role permit authorized actions and reject actions that fall outside the role's assigned permissions?

**Scenario:** Execute a predefined role-action matrix using the prototype roles and selected protected operations, including account administration, audit access, dispatch changes, maintenance changes, and other security-sensitive actions.

**Procedure:** For each role-action pair, attempt the operation under controlled test conditions and compare the observed result with the expected authorization policy.

**Metrics:**

- authorized-action pass rate
- unauthorized-action rejection rate
- number of privilege violations

**Expected observable result:** Authorized actions succeed and unauthorized actions are rejected according to the configured role policy.

**Limitation:** This test validates application-level authorization rules. It does not establish complete organizational compliance or the absence of all security vulnerabilities.

## 12.8 Performance benchmark

The performance benchmark should use reproducible seed datasets and repeatable test procedures.

Recommended measurements include:

- dashboard p50 and p95 response time
- fleet search p50 and p95 response time
- assignment creation p50 and p95 response time
- reconciliation scan duration
- PostgreSQL query latency
- concurrent authenticated sessions
- memory usage
- CPU usage
- database-size growth
- event-history storage growth

Planned performance targets must remain labeled as targets until the completed implementation produces actual measured results.

## 12.9 MEASURABLE METRICS

### a. Correctness and workflow metrics

- invalid-state prevention rate
- duplicate/overlap prevention rate
- exception detection recall
- exception false-positive rate
- state-transition validation pass rate
- audit completeness
- record completeness
- recovery traceability
- authorized-action pass rate
- unauthorized-action rejection rate
- privilege-violation count

### b. Time metrics

- user-interface and API latency
- p95 transaction latency
- update propagation latency
- reconciliation scan duration
- operator task completion time in controlled tests

### c. Resource metrics

- CPU
- memory
- PostgreSQL query latency
- database-size growth per defined number of events
- event-history storage growth
- concurrent session throughput

### Data-quality metrics

- stale-state count
- unresolved exception age
- unverified-report count
- missing-causal-link count
- invalid route-version reference count

---
# REFERENCES

## Philippine government, legal, and institutional sources

Angeles, R. S. L., Cobarrubias, N. A. A., Cipriano, B. M. C., Aguilar, J. E., Bendaña, R. H. N., & Abante, K. I. I. (2024). *Assessing PUV modernization program industry consolidation and financing toward a just transition for transport workers*. WeSolve Foundation, Inc. https://serp-p.pids.gov.ph/publication/public/view?slug=assessing-puv-modernization-program-industry-consolidation-and-financing-towards-just-transition-for

Briones, R. M., Gundaya, D. M., & Domingo, S. N. (2015). *Diagnostic report on the bus transport sector* (Discussion Paper No. 2015-02). Philippine Institute for Development Studies. https://pids.gov.ph/publication/discussion-papers/diagnostic-report-on-the-bus-transport-sector

De Castro, M. (2026, February 23). *Authorities inspect Batangas terminals, ports for safety compliance*. Philippine Information Agency. https://pia.gov.ph/news/authorities-inspect-batangas-terminals-ports-for-safety-compliance/

De Leon, S. (2023, June 19). *MMDA's Bus Management and Dispatch System now monitors EDSA Busway*. Philippine Information Agency. https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway

Dela Cruz, R. C. (2026, January 29). *LTFRB 'rationalizes' 12 PUV routes in NCR, Rizal, Surigao Sur*. Philippine News Agency. https://www.pna.gov.ph/articles/1268016

Department of Transportation. (2021, July 13). *Motor vehicle inspection is mandatory before renewal of registration*. Philippine Information Agency. https://mirror.pia.gov.ph/press-releases/2021/07/13/motor-vehicle-inspection-is-mandatory-before-renewal-of-registration

Guzman, J. (2026, February 4). *LTFRB intensifies roadworthiness campaign on PUVs*. Philippine Information Agency. https://pia.gov.ph/news/ltfrb-intensifies-roadworthiness-campaign-on-puvs/

Land Transportation Franchising and Regulatory Board. (2018, June 28). *Memorandum Circular No. 2018-013: Implementing guidelines in the acceptance of applications for new CPC under the initial implementation of the Public Utility Vehicle Modernization Program*. Supreme Court E-Library. https://elibrary.judiciary.gov.ph/thebookshelf/showdocs/10/90506

Land Transportation Franchising and Regulatory Board. (n.d.). *Terms and conditions: Application for the Land-Based Service Contracting Program*. Retrieved October 5, 2026, from https://ltfrb-scp.ph/terms

Llanto, G. M., & Gerochi, H. A. (2017). *Competition for the market: A policy framework for improving bus operation along EDSA* (Discussion Paper No. 2017-10). Philippine Institute for Development Studies. https://www.pids.gov.ph/publication/discussion-papers/competition-for-the-market-a-policy-framework-for-improving-bus-operation-along-edsa

National Privacy Commission. (2016, August 24). *Implementing rules and regulations of the Data Privacy Act of 2012*. https://privacy.gov.ph/implementing-rules-regulations-data-privacy-act-2012/

Paunan, J. C. (2025, September 25). *DOTr chief wants more PUVs on Commonwealth Avenue*. Philippine Information Agency. https://pia.gov.ph/news/dotr-chief-wants-more-puvs-on-commonwealth-avenue/

Paunan, J. C. (2026a, April 14). *Marcos Jr. gov't launches P1-B service contracting April 15*. Philippine Information Agency. https://pia.gov.ph/news/marcos-jr-govt-launches-p1-b-service-contracting-april-15/

Paunan, J. C. (2026b, April 15). *Service Contracting Program? Here's what you need to know*. Philippine Information Agency. https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/

Pontawe, J., & Napalang, M. A. S. G. L. (2018). *Examining the potential significance of industry consolidation and fleet management in implementing the DOTr's PUV modernization program: A case study of 1TEAM*. Socioeconomic Research Portal for the Philippines. https://serp-p.pids.gov.ph/publication/public/view?slug=examining-the-potential-significance-of-industry-consolidation-and-fleet-management-in-implementing

Presidential Communications Office. (2024, September 24). *Transport agencies, LGUs rationalizing routes for PUVMP*. Philippine Information Agency. https://pia.gov.ph/press-release/transport-agencies-lgus-rationalizing-routes-for-puvmp/

Presidential Communications Office. (2026, April 14). *Malacañang press briefing of Palace Press Officer and PCO Undersecretary Claire Castro with Department of Energy Secretary Sharon Garin, Department of Transportation Acting Secretary Giovanni Lopez, Land Transportation Franchising and Regulatory Board Chairperson Vigor Mendoza II, Land Bank of the Philippines President and Chief Executive Officer Lynette Ortiz, and Department of Finance Undersecretary Karlo Fermin Adriano*. https://pco.gov.ph/press-briefing/malacanang-press-briefing-of-palace-press-officer-and-pco-undersecretary-claire-castro-with-department-of-energy-doe-secretary-sharon-garin-department-of-transportation-dotr-acting-secretary-giov/

Republic Act No. 10173, Data Privacy Act of 2012. (2012). https://officialgazette.gov.ph/2012/08/15/republic-act-no-10173/

## Existing related systems, software, and standards

Fleetbase. (n.d.-a). *Fleetbase: Modular logistics and supply chain operating system* [Computer software]. GitHub. Retrieved October 4, 2026, from https://github.com/fleetbase/fleetbase

Fleetbase. (n.d.-b). *Fleet-Ops: Fleet and transport management extension for Fleetbase* [Computer software]. GitHub. Retrieved October 4, 2026, from https://github.com/fleetbase/fleetops

Fleetbase. (n.d.-c). *Platform overview*. Retrieved October 4, 2026, from https://fleetbase.io/platform

Geotab. (n.d.). *Fleet management software and system for Southeast Asia*. Retrieved October 4, 2026, from https://www.geotab.com/apac/

GIRO. (n.d.). *HASTUS for schedulers*. Retrieved October 4, 2026, from https://www.giro.ca/en-us/our-solutions/hastus-software/hastus-for-schedulers/

GTFS. (n.d.-a). *What is GTFS?* Retrieved October 5, 2026, from https://gtfs.org/getting-started/what-is-gtfs/

GTFS. (n.d.-b). *GTFS Realtime reference*. Retrieved October 5, 2026, from https://gtfs.org/documentation/realtime/reference/

GTFS. (n.d.-c). *Overview*. Retrieved October 4, 2026, from https://gtfs.org/documentation/overview/

Hitachi Mobility Multimodal Solutions. (n.d.). *CleverCAD*. Retrieved October 4, 2026, from https://www.hitachimms.com/products/clevercad/

INIT. (n.d.-a). *Integrated public transport solutions*. Retrieved October 4, 2026, from https://www.initse.com/ende/home/

INIT. (n.d.-b). *Ghost buses: When transit data haunts your riders*. Retrieved October 4, 2026, from https://www.initse.com/enus/news-resources/knowledge-database/2025/ghost-buses-when-transit-data-haunts-your-riders-1/

Optibus. (n.d.-a). *Transportation management software*. Retrieved October 4, 2026, from https://optibus.com/product/

Optibus. (n.d.-b). *Smarter operations management*. Retrieved October 4, 2026, from https://optibus.com/product/operations/

Optibus. (n.d.-c). *Transportation scheduling software*. Retrieved October 4, 2026, from https://optibus.com/product/scheduling/

Samsara. (n.d.-a). *Fleet telematics*. Retrieved October 4, 2026, from https://www.samsara.com/products/telematics

Samsara. (n.d.-b). *Vehicle Gateway*. Retrieved October 4, 2026, from https://www.samsara.com/products/models/vehicle-gateway

Seoul Transport Operation and Information Service. (n.d.). *Bus system (BMS & BIS)*. Retrieved October 4, 2026, from https://topis.seoul.go.kr/openEngBms.do

Trapeze Group. (n.d.). *OPS*. Retrieved October 4, 2026, from https://collaborate2.trapezegroup.com/tag/ops/

---
# ADDITIONAL NOTES

## 1. Evolution of the Project Idea

The project has developed through several planning stages. Earlier concepts were broader and emphasized a fleet-intelligence command dashboard with a live fleet monitor, route schedules, terminal-status views, incidents and alerts, transit analytics, fleet and driver management, static route maps, and manually entered operational information.

Several of those earlier ideas remain relevant because they support the current research-backed direction. The proposed system still benefits from a staff-facing operations dashboard, fleet and driver records, fixed-route information, dispatch assignments, incident handling, maintenance tracking, role-based access control, audit history, PostgreSQL persistence, Docker-based deployment, and measurable software performance.

The current proposal narrows and formalizes those ideas around a more precise systems problem: maintaining a coherent operational record when information changes over time and may be reported manually, arrive late, remain unverified, or conflict with another record.

---
## 2. Former ideas retained because they remain relevant

- browser-based internal operations interface
- fleet-unit management
- driver management
- fixed-route and schedule records
- dispatch assignment management
- incident reporting
- maintenance tracking
- role-based access control
- audit logging
- static route presentation
- PostgreSQL relational storage
- database migrations
- Docker deployment
- controlled simulated-data testing
- software performance benchmarks

---
## 3. Former ideas narrowed or reclassified

### a. Fleet intelligence

The earlier use of the term "fleet intelligence" was broader than the evidence justified. The current proposal uses descriptive analytics, monitoring, validation, and reconciliation terminology unless a specific higher-level analytical function is implemented and tested.

### b. Live fleet monitor

The dashboard may display the latest known operational state, but it should not imply live physical tracking when there is no GPS, AVL, or telemetry source.

### c. Terminal status and passenger conditions

Terminal-level passenger counts, crowding, arrival estimates, and similar fields should be included only when there is a defined data source and a clear workflow. They should not be presented as automatically observed information.

### d. ETA

Any ETA entered by staff is a reported estimate. It must not be described as telemetry-derived or predictive ETA.

### e. Maintenance intelligence

Maintenance state and work history are within scope. Predictive diagnostics are not supported without connected diagnostic data.

### f. Cost advantage

Self-hosting and the absence of required telemetry hardware in the core prototype describe architecture choices. They do not establish that the system is cheaper than commercial products.

---
## 4. Latest research-backed additions

The current proposal adds several concepts that are more directly connected to the verified evidence:

- effective-dated route versions
- separation of administrative state, availability state, and assignment state
- source provenance for operational reports
- verification status
- freshness and stale-record handling
- current-state projection supported by event history
- deterministic application-level consistency rules
- exception and human-resolution workflow
- stronger traceability from evidence to requirement, feature, test, and metric
- explicit distinction between what the system records, what it infers, and what it cannot independently know
- optional future interoperability through GTFS or GTFS-Realtime where justified

The result is a more precise project that retains useful software-engineering ideas from earlier planning while removing unsupported claims and separating future integrations from current capabilities.

---

## 5. Some corrections the first features that were claimed

This appendix records claims from earlier planning material that should not be reused without stronger evidence.

### a. Comparative pricing

Public evidence reviewed is insufficient to establish comparative Philippine procurement cost. The proposed system can be described as self-hostable and as not requiring proprietary GPS hardware for its core manual workflow, but it has not been proven cheaper.

### b. Competitor feature absence

The public materials reviewed establish documented capabilities but do not establish whether comparable internal consistency or configurable workflow mechanisms are absent.

### c. Offline capability

A self-hosted PostgreSQL application is not automatically a complete offline system. Users still require network access to the application host. Offline client behavior, local synchronization, conflict resolution, and store-and-forward operation require separate architecture.

### d. Safety wording

A stale trip record should first be described as an operational freshness or record-consistency problem. It should not automatically be called a safety event unless a defined safety process or supporting evidence establishes that relationship.

### e. Append-only history and immutability

Append-only application logic improves traceability, but it does not create absolute immutability. A sufficiently privileged database administrator or infrastructure administrator can alter stored data unless stronger technical and governance controls are implemented.

**append-only business-event history**

---

## 6. What the system knows, infers and cannot know

### a. Vehicle-unavailable report example

### What the system knows

An authenticated Operations Staff account submitted at 08:14 that vehicle `B-17` was unavailable, effective at 08:10, and the application stored the report with its source and timestamp.

### What the system can infer

Under the configured dispatch rule, `B-17` should not receive a new assignment while its recorded availability remains unavailable.

### b. What the system cannot independently know

The application cannot determine whether `B-17` is physically broken or otherwise truly unavailable without an independent observation, inspection, telemetry source, or verification process.

## c. Stale active-trip example

### What the system knows

Trip `T-402` remains recorded as `IN_SERVICE`, its planned end time was 10:30, and no completion event has been recorded by 12:00.

### What the system can infer

The operational record may be stale or incomplete and should be presented for review.

### What the system cannot independently know

The application cannot determine whether the bus is still physically operating, has completed the trip, or experienced an unreported incident.

---


--> delete from here


## SOURCE VERIFICATION QUICK INDEX - checking if the data source is reliable

The table below uses recognizable source names rather than internal identifiers. Each phrase can be searched with Ctrl+F on the linked source.

| Source | Ctrl+F phrase |
|---|---|
| PIA - *DOTr chief wants more PUVs on Commonwealth Avenue* | `3,972 public utility vehicles` |
| PIA - *Marcos Jr. gov't launches P1-B service contracting April 15* | `reduced their runs` |
| PNA - *LTFRB 'rationalizes' 12 PUV routes in NCR, Rizal, Surigao Sur* | `modified 12 public utility vehicle` |
| PIA - *MMDA's Bus Management and Dispatch System now monitors EDSA Busway* | `systematic monitoring of the number of units` |
| Pontawe and Napalang (2018) | `industry consolidation and fleet management are both significant components` |
| Angeles et al. (2024) | `diverse experiences of cooperatives` |
| PIA - *Service Contracting Program? Here's what you need to know* | `Two monitoring systems are in place` |
| Presidential Communications Office - April 14, 2026 press briefing | `verification and validation becomes very difficult` |
| PIA - *LTFRB intensifies roadworthiness campaign on PUVs* | `condition of tires, lights, seats` |
| PIA - *Authorities inspect Batangas terminals, ports for safety compliance* | `thin or worn-out tires` |
| Republic Act No. 10173 | `licenses or its denials, suspension or revocation` |
| Samsara Fleet Telematics | `Routing & dispatch` |
| Geotab APAC | `Track vehicles and assets in real time` |
| Trapeze OPS | `OPS streamlines and automates day-to-day tasks for bidding, dispatching, timekeeping, and workforce management` |
| Optibus Operations | `Manage allocation, dispatch, payroll` |
| INIT - *Ghost Buses* | `executed in the field but never modified in the central system` |
| HASTUS for schedulers | `unit availability` |
| CleverCAD | `Event-Driven Incident Management` |
| Seoul TOPIS | `collects, processes, and analyzes bus location information in real-time` |
| Fleetbase | `open-source, modular operating system for logistics and supply chain operations` |
| GTFS Overview | `Trip updates - delays, cancellations, changed routes` |


## APPENDIX E. DESIGN PRINCIPLE

> **The proposed system should make operational records coherent, traceable, and testable without treating software-level consistency as proof of real-world truth.**

# OPEN RESEARCH QUESTIONS

1. What exact workflows do target Philippine provincial/private bus operators currently use for dispatch, incident reporting, and maintenance?
2. Do target operators already use an ERP, spreadsheet, radio log, fleet SaaS, GPS provider portal, or custom dispatch system?
3. What data is entered by dispatcher, driver, terminal staff, maintenance staff, or supervisor?
4. Which operational changes require supervisor verification?
5. What event is authoritative when two staff reports conflict?
6. What freshness threshold is meaningful for different record types?
7. Which driver fields are genuinely required, and which should be excluded for data minimization?
8. Are route definitions maintained internally, imported from LTFRB records, or represented another way?
9. Would a target deployment need GTFS/GTFS-Realtime interoperability?
10. Is local or LAN deployment actually needed, or is standard cloud or virtual-machine hosting sufficient?
11. What backup, retention, and disaster-recovery requirements would a real operator accept?
12. Which exception rules produce useful alerts without alert fatigue?
13. Is there a Philippine operator willing to validate terminology and workflows without requiring production deployment?

---

# TRACEABILITY MATRIX

The traceability matrix uses recognizable source names and descriptive evaluation names. This avoids internal source codes and experiment identifiers that make the document harder to review manually.

| Evidence source | Constraint | Requirement | Proposed feature | Evaluation | Metric |
|---|---|---|---|---|---|
| Philippine Information Agency, *DOTr chief wants more PUVs on Commonwealth Avenue* | Administrative fleet count does not establish actual operating availability | Separate fleet identity from current availability | Vehicle availability state and history | Invalid assignment prevention test | invalid-assignment rejection rate |
| Philippine Information Agency, *Marcos Jr. gov't launches P1-B service contracting April 15* | Planned service can differ from actual service | Preserve service changes without deleting the original plan | Trip lifecycle and cancellation or curtailment events | Recovery traceability test | causal-link completeness |
| Philippine News Agency, *LTFRB 'rationalizes' 12 PUV routes in NCR, Rizal, Surigao Sur* | Route definitions can change | Preserve effective-dated route definitions | Route versioning | Invalid assignment prevention test | invalid route-version rejection rate |
| Philippine Information Agency, *MMDA's Bus Management and Dispatch System now monitors EDSA Busway* | Dispatch is an operational workflow | Preserve assignment actor, resources, times, and state | Dispatch board and assignment lifecycle | Invalid assignment prevention test and audit completeness test | validation pass rate and audit completeness |
| Pontawe and Napalang (2018); Angeles et al. (2024) | Shared organizations can require common fleet management | Maintain a common resource view | Fleet, driver, route, and dispatch modules | Record completeness test | completeness percentage |
| Philippine Information Agency, *Service Contracting Program? Here's what you need to know*; Presidential Communications Office press briefing, April 14, 2026 | Operational data can come from different sources | Preserve source and verification context | Operational-report provenance | Reconciliation detection test and audit completeness test | detection precision and audit completeness |
| Philippine Information Agency, *LTFRB intensifies roadworthiness campaign on PUVs*; *Authorities inspect Batangas terminals, ports for safety compliance* | A vehicle can remain a fleet record while being unusable | Gate dispatch by recorded availability and maintenance state | Vehicle availability and maintenance workflow | Invalid assignment prevention test and reconciliation detection test | prevention and detection rate |
| Republic Act No. 10173 and its implementing rules | Driver and staff information requires protection | Apply least privilege and appropriate safeguards | RBAC, secure processing, and auditing | Authorization and access-control test | unauthorized-action rejection rate and privilege-violation count |
| INIT, *Ghost Buses: When Transit Data Haunts Your Riders* | Field changes can diverge from central operational data in mature transit environments | Preserve change events and detect stale or inconsistent application state | Reconciliation and exception queue | Reconciliation detection test and recovery traceability test | detection recall and traceability |

---
# FINAL RECOMMENDATIONS

## IMPLEMENT NOW

- Keep the revised title and narrow system position.
- Implement explicit operational state and event history.
- Separate vehicle availability from trip assignment state.
- Add provenance to manual operational reports.
- Add effective-dated route versions.
- Keep dispatch, incident, and maintenance as transactional workflows.
- Implement deterministic validation and exception rules.
- Add human resolution workflow for exceptions.
- Keep analytics descriptive and monitoring-oriented.
- Keep the modular monolith architecture with PostgreSQL and Flyway.
- Test with reproducible scenarios and report actual metrics.

## INVESTIGATE FURTHER

- real operator workflow interviews
- local data fields and terminology
- retention and privacy policy
- route-data source
- practical freshness thresholds
- role granularity
- GTFS interoperability

## FUTURE INTEGRATION

- GPS or AVL
- GTFS-Realtime
- external identity provider
- API ecosystem
- external maintenance systems
- passenger information
- business intelligence and time-series analytics

## DO NOT IMPLEMENT OR OUT OF SCOPE

- AI chatbot
- ML forecasting
- predictive maintenance
- autonomous dispatch
- digital twin
- advanced scheduling optimizer
- city traffic optimization
- proprietary telemetry stack
- fare and payment system

---

# SOURCE AUDIT TABLE

The source audit table uses recognizable source names instead of internal source codes. Each URL below was checked during the current revision. Where an earlier link redirected to a newer official page, the current official page is used.

| Source | Claim supported | Organization | Date | Direct URL | Source type | Evidence strength | Status |
|---|---|---|---|---|---|---|---|
| *DOTr chief wants more PUVs on Commonwealth Avenue* | Franchised count versus actual operating supply | Philippine Information Agency | 2025-09-25 | https://pia.gov.ph/news/dotr-chief-wants-more-puvs-on-commonwealth-avenue/ | Government public-information report | High, case-specific | Current and accessible |
| *Marcos Jr. gov't launches P1-B service contracting April 15* | Trips reduced or stopped during fuel-price shock | Philippine Information Agency | 2026-04-14 | https://pia.gov.ph/news/marcos-jr-govt-launches-p1-b-service-contracting-april-15/ | Government public-information report | High, event-specific | Current and accessible |
| *LTFRB 'rationalizes' 12 PUV routes in NCR, Rizal, Surigao Sur* | Route modification and continuing route study | Philippine News Agency | 2026-01-29 | https://www.pna.gov.ph/articles/1268016 | Government news agency report | Very high | Current and accessible |
| *MMDA's Bus Management and Dispatch System now monitors EDSA Busway* | Existing Philippine digital dispatch monitoring | Philippine Information Agency | 2023-06-19 | https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway | Government public-information report | High | Accessible |
| *Examining the Potential Significance of Industry Consolidation and Fleet Management in Implementing the DOTr's PUV Modernization Program: A Case Study of 1TEAM* | Consolidation and fleet-management mechanism | UP School of Urban and Regional Planning, indexed by SERP-P | 2018 | https://serp-p.pids.gov.ph/publication/public/view?slug=examining-the-potential-significance-of-industry-consolidation-and-fleet-management-in-implementing | Institutional research | Moderate to high, context-specific | Accessible |
| *Assessing PUV Modernization Program Industry Consolidation and Financing toward a Just Transition for Transport Workers* | Diverse cooperative consolidation experiences | WeSolve Foundation, Inc., indexed by SERP-P | 2024 | https://serp-p.pids.gov.ph/publication/public/view?slug=assessing-puv-modernization-program-industry-consolidation-and-financing-towards-just-transition-for | Institutional research | High, context-specific | Accessible |
| *Service Contracting Program? Here's what you need to know* | GPS and manual monitoring used together | Philippine Information Agency | 2026-04-15 | https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/ | Government public-information report | Very high | Current and accessible |
| Malacañang press briefing, April 14, 2026 | Manual-monitoring verification difficulty and GPS preference | Presidential Communications Office | 2026-04-14 | https://pco.gov.ph/press-briefing/malacanang-press-briefing-of-palace-press-officer-and-pco-undersecretary-claire-castro-with-department-of-energy-doe-secretary-sharon-garin-department-of-transportation-dotr-acting-secretary-giov/ | Official transcript | Very high | Current and accessible |
| *LTFRB intensifies roadworthiness campaign on PUVs* | Roadworthiness inspection fields | Philippine Information Agency | 2026-02-04 | https://pia.gov.ph/news/ltfrb-intensifies-roadworthiness-campaign-on-puvs/ | Government public-information report | Very high | Current and accessible |
| *Authorities inspect Batangas terminals, ports for safety compliance* | Worn tires and missing fire extinguishers | Philippine Information Agency | 2026-02-23 | https://pia.gov.ph/news/authorities-inspect-batangas-terminals-ports-for-safety-compliance/ | Government public-information report | High, case-specific | Current and accessible |
| *Competition for the Market: A Policy Framework for Improving Bus Operation along EDSA* | Historical bus-dispatch and regulatory context | Philippine Institute for Development Studies | 2017-04-11 | https://www.pids.gov.ph/publication/discussion-papers/competition-for-the-market-a-policy-framework-for-improving-bus-operation-along-edsa | Institutional discussion paper | Historical context only | Accessible |
| *Diagnostic Report on the Bus Transport Sector* | Historical bus-sector structure | Philippine Institute for Development Studies | 2015-01-06 | https://pids.gov.ph/publication/discussion-papers/diagnostic-report-on-the-bus-transport-sector | Institutional discussion paper | Historical context only | Accessible |
| Republic Act No. 10173 | Sensitive personal information and security obligations | Republic of the Philippines | 2012-08-15 | https://officialgazette.gov.ph/2012/08/15/republic-act-no-10173/ | Law | Very high | Current law |
| *Implementing Rules and Regulations of the Data Privacy Act of 2012* | Privacy and security implementation requirements | National Privacy Commission | 2016-08-24 | https://privacy.gov.ph/implementing-rules-regulations-data-privacy-act-2012/ | Official regulation | Very high | Current official page |
| Samsara Fleet Telematics | GPS, diagnostics, dispatch, and workflows | Samsara | Retrieved 2026-10-04 | https://www.samsara.com/products/telematics | Official vendor documentation | High for product capability | Current and accessible |
| Samsara Vehicle Gateway | Connected hardware data source | Samsara | Retrieved 2026-10-04 | https://www.samsara.com/products/models/vehicle-gateway | Official vendor documentation | High | Current and accessible |
| Geotab APAC | Southeast Asia and Philippines telematics capabilities | Geotab | Retrieved 2026-10-04 | https://www.geotab.com/apac/ | Official vendor documentation | High | Current and accessible |
| Trapeze OPS documentation | Dispatch, timekeeping, and workforce management | Trapeze Group | Retrieved 2026-10-04 | https://collaborate2.trapezegroup.com/tag/ops/ | Official documentation | High | Current and accessible |
| Optibus Transportation Management Software | Public-transport planning and operations | Optibus | Retrieved 2026-10-04 | https://optibus.com/product/ | Official vendor documentation | High | Current and accessible |
| Optibus Operations Management | Allocation, dispatch, payroll, and operational adjustment | Optibus | Retrieved 2026-10-04 | https://optibus.com/product/operations/ | Official vendor documentation | High | Current and accessible |
| INIT, *Ghost Buses: When Transit Data Haunts Your Riders* | Field changes not reflected in central operational data | INIT | Retrieved 2026-10-04 | https://www.initse.com/enus/news-resources/knowledge-database/2025/ghost-buses-when-transit-data-haunts-your-riders-1/ | Industry architectural evidence | High for the described mechanism | Accessible |
| HASTUS for schedulers | Timetables, vehicle blocks, crew scheduling, and unit availability | GIRO | Retrieved 2026-10-04 | https://www.giro.ca/en-us/our-solutions/hastus-software/hastus-for-schedulers/ | Official vendor documentation | High | Current and accessible |
| CleverCAD | CAD/AVL, incidents, and disruption management | Hitachi Mobility Multimodal Solutions | Retrieved 2026-10-04 | https://www.hitachimms.com/products/clevercad/ | Official vendor documentation | High | Current canonical page |
| Seoul TOPIS Bus System | BMS and BIS and real-time bus-location processing | Seoul Transport Operation and Information Service | Retrieved 2026-10-04 | https://topis.seoul.go.kr/openEngBms.do | Government system documentation | Very high | Current and accessible |
| Fleetbase | Open-source modular logistics platform | Fleetbase | Retrieved 2026-10-04 | https://github.com/fleetbase/fleetbase | Official project repository | High | Current and accessible |
| Fleet-Ops | Dispatch, fleet, live operations, and maintenance | Fleetbase | Retrieved 2026-10-04 | https://github.com/fleetbase/fleetops | Official project repository | High | Current and accessible |
| GTFS Overview | Schedule and Realtime interoperability | GTFS documentation | Retrieved 2026-10-04 | https://gtfs.org/documentation/overview/ | Standard documentation | Very high | Current and accessible |
| LTFRB Memorandum Circular No. 2018-013 | Route authorization remains subject to route rationalization or applicable LPTRP | Land Transportation Franchising and Regulatory Board, reproduced by Supreme Court E-Library | 2018-06-28 | https://elibrary.judiciary.gov.ph/thebookshelf/showdocs/10/90506 | Official regulatory issuance | High for historical policy context | Accessible |
| *Transport agencies, LGUs rationalizing routes for PUVMP* | Route rationalization and LPTRP planning context | Philippine Information Agency / Presidential Communications Office | 2024-09-24 | https://pia.gov.ph/press-release/transport-agencies-lgus-rationalizing-routes-for-puvmp/ | Government public-information report | High for policy context | Current and accessible |
| *Motor vehicle inspection is mandatory before renewal of registration* | Inspection and roadworthiness before registration renewal | Philippine Information Agency / Department of Transportation | 2021-07-13 | https://mirror.pia.gov.ph/press-releases/2021/07/13/motor-vehicle-inspection-is-mandatory-before-renewal-of-registration | Government press release | High for regulatory context | Accessible |
| LTFRB Land-Based Service Contracting Program Terms and Conditions | Registration, roadworthiness, trip-report, service-scope, and audit-trail requirements within the program | Land Transportation Franchising and Regulatory Board | Retrieved 2026-10-05 | https://ltfrb-scp.ph/terms | Official program terms | Very high within program scope | Current and accessible |
| GTFS, *What is GTFS?* | Definition of GTFS Schedule and GTFS Realtime | GTFS documentation | Retrieved 2026-10-05 | https://gtfs.org/getting-started/what-is-gtfs/ | Standard documentation | Very high | Current and accessible |
| GTFS Realtime Reference | Realtime feed relationship, trip updates, vehicle positions, alerts, and experimental fields | GTFS documentation | Retrieved 2026-10-05 | https://gtfs.org/documentation/realtime/reference/ | Standard documentation | Very high | Current and accessible |
