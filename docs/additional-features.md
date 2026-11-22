---
title: "Arc Transit: Additional Features Plan"
aliases:
  - Arc Transit Additional Features
tags:
  - arc-transit
  - enterprise-software
  - public-transport
  - systems-engineering
  - philippines
  - operations
  - analytics
  - decision-support
  - automation
  - data-quality
  - research
status: draft
parent: "[[research]]"
date_created: 2026-10-05
date_researched: 2026-10-05
---

# Arc Transit Additional Features

An operational-intelligence extension to Arc Transit that converts existing traceable operational records into historical analytics, data-quality monitoring, deterministic alerts, rule-based decision support, limited workflow automation, change-impact visibility, and unified event reconstruction while retaining human operational authority and the manual-first data assumptions of the parent system.

---

# I. SUMMARY

The existing Arc Transit system contains the following features:

→ fleet and driver records
→ effective-dated route definitions
→ trip and dispatch workflows
→ incidents and maintenance
→ manual operational reports with provenance
→ current-state projection plus append-only business-event history
→ role-based access control
→ auditing
→ deterministic validation
→ reconciliation and exception management
→ descriptive operational monitoring

The next iteration of development will focus adding new features in the Arc Transit System into:

→ **structured operational data**
→ **better operational analytics**
→ **continuous data-quality monitoring**
→ **rule-based decision support and prioritization**
→ **automated deterministic workflow**
→ **change-impact visibility**
→ **better event reconstruction and audit use**

The recommended additions are therefore:

| Feature                                               | Recommendation             | Evidence position                                    |
| ----------------------------------------------------- | -------------------------- | ---------------------------------------------------- |
| Detailed operational analytics                        | **IMPLEMENT**              | Strong                                               |
| Operational record/data-quality monitoring            | **IMPLEMENT**              | Very strong                                          |
| Transparent rule-based DSS + exception prioritization | **IMPLEMENT**              | Strong                                               |
| Deterministic workflow automation                     | **IMPLEMENT**              | Strong                                               |
| Change-impact analysis                                | **SUPPORTING — IMPLEMENT** | Strong design fit; moderate direct evidence          |
| Operational event timeline/reconstruction             | **SUPPORTING — IMPLEMENT** | Strong traceability fit                              |
| Scenario / what-if analysis                           | **FUTURE**                 | Useful but not required yet                          |
| External fuel/weather/regulatory context              | **FUTURE / OPTIONAL**      | Relevant, but adds integration/provenance complexity |
| Statistical forecasting                               | **FUTURE**                 | Requires enough historical data and validation       |
| GTFS/API expansion                                    | **FUTURE / OPTIONAL**      | Interoperability benefit, not a current core need    |
| ML anomaly detection                                  | **OUT OF SCOPE**           | Current deterministic rules are more defensible      |
| AI/LLM operations assistant                           | **OUT OF SCOPE**           | No demonstrated need                                 |
| Predictive maintenance                                | **OUT OF SCOPE**           | No diagnostic/telemetry dataset                      |
| Autonomous dispatch                                   | **OUT OF SCOPE**           | Changes project authority and risk boundary          |
| Digital twin                                          | **OUT OF SCOPE**           | Excessive scope/infrastructure                       |
| Microservices/Kafka solely for analytics              | **OUT OF SCOPE**           | No demonstrated architectural need                   |

## SUMMARY OF ADDITONAL FEATURES

### 1. Analytics

Operational events accumulate  
→ raw records become difficult to interpret at scale  
→ trends and repeated causes are harder to identify  
→ staff must perform manual aggregation  
→ analytics summarizes and links aggregates back to evidence.

### 2. Record health

Human/multi-source data changes over time  
→ some records become stale/incomplete/unverified  
→ operational dashboard may display technically valid but weak information  
→ decisions may rely on poor-quality records  
→ record-health metrics expose quality conditions before interpretation.

### 3. Lightweight Transparent Rule-based DSS

Many exceptions compete for staff attention  
→ important and low-impact items appear together  
→ staff attention becomes fragmented  
→ high-impact items can be delayed  
→ deterministic rules prioritize and explain what requires review.

### 4. Simple Automated Task Delegation

Known checks are repeated manually  
→ staff spend time re-checking predictable conditions  
→ omissions are possible  
→ scheduled deterministic checks generate the appropriate review items automatically.

### 5. Impact of Changes

Resource or route state changes  
→ dependent assignments become invalid or questionable  
→ user may not know every dependency  
→ impact preview identifies directly affected records.

### 6. Timeline

Related events are stored in separate modules  
→ incident history must be reconstructed manually  
→ review becomes slow and error-prone  
→ unified event timeline presents the causal sequence.

---
# II. ADDITIONS THAT ARE OUT OF SCOPE

This iteration intentionally excludes:

→ machine-learning forecasting;
→ autonomous dispatch;
→ predictive maintenance;
→ city-scale traffic management;
→ real-time GPS tracking unless separately integrated;
→ mathematical scheduling optimization; and
→ opaque AI recommendations.

## 1. AI chatbot / LLM operations assistant

Reason:
- no demonstrated operational need
- introduces hallucination/governance risk
- deterministic access to known operational facts is sufficient.

## 2. ML anomaly detection

Reason:
- Arc already has interpretable known-rule violations
- no validated anomaly dataset
- false positives could produce unnecessary operational burden

## 3. Predictive maintenance

Reason:
- no engine/sensor/diagnostic dataset.

## 4. Autonomous dispatch

Reason:
- changes human authority;
- requires much stronger operational modeling and safety validation.

## 5. Digital twin

Reason:
- requires higher-fidelity state synchronization and modeling than Arc possesses.

## 6. Microservices/event streaming for appearance

Reason:
- current load and architecture do not establish the need.

---
# III. DEFENSIBLE PROJECT OPPORTUNITY

The additional iteration is not novel because "no system has analytics." Mature systems clearly already provide analytics, alerts, decision support, validations, and workflow automation. The defensible Arc-specific opportunity is narrower:

 **A coherent, provenance-aware, human-entered operational record already built by Arc to provide lightweight analytics, record-health visibility, deterministic prioritization, and automation without requiring a telemetry-first or advanced optimization environment.**

The research contribution is therefore primarily:

→ integration
→ operating-model fit
→ transparency
→ explicit boundaries
→ traceability
→ measurable implementation

The strongest measurable external evidence found for a transit decision-support pattern is the U.S. Federal Transit Administration's Pace Transit Operations Decision Support System (TODSS) evaluation. In one compared operational day, dispatcher-facing data messages were reduced from **7,927** in the pre-TODSS comparison to **2,515** in TODSS. The report interpreted the event-processing result as roughly one-third the number of incidents presented to dispatchers. It also reported a **20% overall decline in driver-dispatcher communications**, while separately finding that average Request-to-Talk response time did **not** improve in that comparison (86 seconds before versus 88 seconds with TODSS). Response-time variability, however, decreased from a standard deviation of 253.11 seconds to 171.67 seconds. This is useful because it shows a realistic pattern: prioritization and automated filtering can reduce information clutter without guaranteeing improvement in every operational metric.

However, this extension therefore does **not** claim that adding analytics, DSS, or automation automatically makes arc transit system overall better. The research supports testing whether these additions can improve specific software-level outcomes such as:

→ time required to locate important operational information
→ number of manual checking steps
→ exception detection precision and recall
→ time required to determine affected assignments
→ completeness of operational records
→ age of unresolved exceptions
→ time required to reconstruct an incident
→ analytics query latency
→ rule-evaluation latency
→ CPU and memory use
→ database growth
→ user task completion accuracy

==🔵The parent document,== [[research]], establishes Arc Transit as a **manual-first operational coordination and operational-state reconciliation application** for a single Philippine bus operator/cooperative prototype.

This extension accepts the following parent boundaries as fixed unless later research explicitly changes them:

1. Arc does not independently know physical vehicle location without an external source.
2. Arc does not independently determine roadworthiness.
3. Staff-entered data remains a legitimate but limited source of operational state.
4. Provenance, verification status, freshness, and audit history matter.
5. Deterministic reconciliation is preferred over unsupported AI claims.
6. Route versions and operational state must preserve history.
7. The primary architecture remains a modular monolith using Java/Spring/PostgreSQL.
8. The application is not positioned as a replacement for CAD/AVL, Samsara, Geotab, Optibus, HASTUS, INIT, CleverCAD, or city-scale systems.
9. Software-level tests are the main defensible evaluation method.
10. Real-world outcome claims require actual deployment evidence.

---

# IV. CURRENT PHILIPPINE CONTEXTS RELEVANT TO THE PROJECT

## 4.1 Operational service can diverge from plan

The parent research documented a 2026 Philippine fuel-price shock in which some PUV operators reportedly reduced runs or stopped operating. This establishes a concrete reason for distinguishing planned service from recorded actual operational state.

**Iteration implication:** once Arc stores these changes reliably, historical summaries and reason-based analytics become useful.

Source:
- Philippine Information Agency, *Marcos Jr. gov't launches P1-B service contracting April 15*  
  https://pia.gov.ph/news/marcos-jr-govt-launches-p1-b-service-contracting-april-15/

## 4.2 Manual monitoring creates verification limits

The parent research documented official 2026 use of both GPS and manual monitoring in the Service Contracting Program, while an official press briefing stated that manual monitoring made verification and validation difficult.

**Iteration implication:** analytics should not merely summarize operations; Arc should also expose the quality, freshness, source, and verification status of the data being summarized.

Sources:
- Philippine Information Agency, *Service Contracting Program? Here's what you need to know*  
  https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/
- Presidential Communications Office, April 14, 2026 press briefing  
  https://pco.gov.ph/press-briefing/malacanang-press-briefing-of-palace-press-officer-and-pco-undersecretary-claire-castro-with-department-of-energy-doe-secretary-sharon-garin-department-of-transportation-dotr-acting-secretary-giov/

## 4.3 Philippine digital dispatch already exists

The MMDA Bus Management and Dispatch System is already documented in the parent research.

**Iteration implication:** Arc must not claim that digital dispatch, operational monitoring, or dashboards are novel. The opportunity is to improve how Arc uses its own already-collected operational data.

Source:
- Philippine Information Agency, *MMDA's Bus Management and Dispatch System now monitors EDSA Busway*  
  https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway

## 4.4 Current Philippine policy context uses dashboards and monitoring

The 2025 General Appropriations Act provision for the Public Transport Modernization Program required a **real-time public dashboard** displaying outputs and outcomes of PTMP fund use. This is a government-program transparency context rather than internal bus operations, but it shows that dashboard-based monitoring is an established contemporary Philippine information-management pattern.

Source:
- DOTr / GAA 2025 copy hosted by LTFRB  
  https://ltfrb.gov.ph/wp-content/uploads/2025/01/DOTR_GAA-2025.pdf

**Boundary:** this does not establish that a private bus operator requires the same dashboard or the same indicators.

## 4.5 Philippine transport analytics/DSS is an active implementation area

In May 2026, DOST-Calabarzon and Lipa City announced the SEERMO traffic analytics and decision-support project, which was intended to analyze CCTV, administrative records, and field validation to support traffic planning. The project had a stated target of reducing peak-hour travel time by 15–20%, but at the time of the report this was an **aim**, not an achieved result.

Source:
- Philippine News Agency, *Lipa City's AI traffic system aims to cut peak-hour travel by 15-20%*  
  https://www.pna.gov.ph/articles/1275408

**Project relevance:** the source supports the relevance of analytics and decision-support concepts in a current Philippine transport context.

**Boundary:** Arc should not copy the AI approach or the 15–20% target. Arc has different users, data, infrastructure, and evaluation boundaries.

## 4.6 New Philippine bus systems are using operations-control concepts

A 2025 PIA report on the Davao City Interim Bus System described an operations control center planned with fleet monitoring, real-time data tracking, inter-agency linkages, vehicle inspection facilities, and maintenance support.

Source:
- Philippine Information Agency, *Davao City Interim Bus System garage taking shape*  
  https://pia.gov.ph/news/davao-city-interim-bus-system-garage-taking-shape/

**Boundary:** this is a city-backed bus system with different infrastructure assumptions. It is evidence of current operations-control direction, not evidence that Arc should reproduce an OCC or real-time tracking stack.

---

# V. RELATED LITERATURE AND SYSTEM EVIDENCES

## 5.1 Transit visual analytics: PubtraVis

Prommaharaj et al. developed PubtraVis using GTFS data to visualize public-transport operational characteristics including mobility, speed, flow, density, headway, and statistical analysis. The study reports that the tool was tested with **122 real-world users** and was generally found useful and easy to start using, while "easy to use" still required improvement.

Source:
- Prommaharaj et al. (2020), *Visualizing public transit system operation with GTFS data: A case study of Calgary, Canada*  
  https://pmc.ncbi.nlm.nih.gov/articles/PMC7160583/

**Relevance to Arc:** operational data becomes more useful when users can compare and inspect patterns rather than only open individual CRUD records.

**Boundary:** PubtraVis relies on GTFS/transit movement data and does not validate Arc's exact metrics or manual-first operating model.

## 5.2 Public-transport visual analytics: G2Viz

A 2024 *Public Transport* article introduced G2Viz, a browser-based tool for visualizing and analyzing public-transit systems from GTFS data. Its stated motivation is the shift from merely collecting transit data toward deriving actionable insights and communicating complex findings to decision makers.

Sources:
- Urban Analytics Lab publication page  
  https://ual.sg/publication/2024-pt-g-2-viz/
- Article DOI metadata / *Public Transport*  
  https://doi.org/10.1007/s12469-024-00362-x

**Relevance to Arc:** reinforces the idea that storing transport data alone is not the endpoint; analysis and presentation are separate capabilities.

## 5.3 Dashboard information quality and decision making

A 2024 experiment in *Information & Management* used **524 participants** and found that dashboard information format, currency, and completeness indirectly affected decision-making quality through lower perceived task complexity and greater information satisfaction.

Source:
- Hjelle, Mikalef, Altwaijry, & Parida (2024), *Organizational decision making and analytics: An experimental study on dashboard visualizations*  
  https://doi.org/10.1016/j.im.2024.104011

**Relevance to Arc:** analytics should not be designed as decorative charts. Currency, completeness, and understandable presentation matter.

**Boundary:** this is general organizational decision-making research, not a Philippine transit deployment.

## 5.4 Public-transport data quality and decision domains

A 2025 study in *Research in Transportation Business & Management* evaluated data sources for public-transport decision-making. It discusses accuracy, completeness, consistency, timeliness, granularity, and richness of information as important data characteristics for planning, operations, and performance measurement.

Source:
- Urbano, Arena, & Azzone (2025), *Big data for decision-making in public transport management: A comparison of different data sources*  
  https://doi.org/10.1016/j.rtbm.2025.101298

**Relevance to Arc:** Arc's analytics should make its input quality visible, especially because Arc explicitly allows human-entered records.

## 5.5 Transit data quality can be measured continuously

The Mineta Transportation Institute's 2024 work on GTFS accuracy proposed scalable temporal and spatial accuracy metrics intended to help transit agencies continuously assess data quality.

Source:
- Newmark (2024), *Assessing GTFS Accuracy*  
  https://transweb.sjsu.edu/research/2017-Public-Transit-Statistical-Analysis

**Relevance to Arc:** although Arc is not primarily GTFS-based, the broader pattern is directly relevant: operational information quality should be measured, not assumed.

## 5.6 Traffic data-quality monitoring with explicit thresholds

A 2026 *Future Transportation* study proposed a traffic data-quality framework with ingestion, quality assessment, correction, and alerting. It explicitly describes user-configurable alert thresholds, for example triggering an alert when completeness remains below a defined threshold for a defined duration.

Source:
- *Data Quality in Traffic Management: Framework and Real-World Impacts* (2026)  
  https://doi.org/10.3390/futuretransp6030124

**Relevance to Arc:** supports explicit, transparent data-health thresholds rather than opaque "confidence" claims.

**Boundary:** the paper's correction/imputation mechanisms and traffic-signal use case are outside Arc's current scope. Arc should detect and surface quality issues rather than automatically invent or impute operational truth.

---

# VI. Federal Transit Administration's Transit Operations Decision Support System (TODSS)

The most directly relevant evaluated architecture found is the U.S. Federal Transit Administration's Transit Operations Decision Support System (TODSS) prototype at Pace Suburban Bus.

Although the evaluation is historical, it remains useful architectural evidence because it directly studied:

- dispatcher information overload;
- incident prioritization;
- event-processing rules;
- action plans;
- automatic incident expiration;
- audit history;
- access to supporting information;
- dispatcher performance; and
- communications volume.

Primary report:
- Hiller & Luck (2009), *Transit Operations Decision Support System Core Requirements Evaluation and Update Recommendations*  
  https://rosap.ntl.bts.gov/view/dot/4373  
  PDF: https://www.transit.dot.gov/sites/fta.dot.gov/files/docs/Technical_Memorandum_TODSS_Core_Requirements_Evaluation_and_Update_Recommendations_Final_Report_v4_%28final%29_%282%29.pdf

The report compared operational data from pre-TODSS and TODSS periods. One reported one-day comparison showed:

| Measure | Pre-TODSS | TODSS |
|---|---:|---:|
| Dispatcher-facing data messages | 7,927 | 2,515 |
| Average RTT response time | 86 s | 88 s |
| Median RTT response time | 14 s | 18 s |
| Response-time standard deviation | 253.11 s | 171.67 s |

The report described the message reduction as approximately one-third the incident volume of the old system and attributed practical value to reducing clutter so important incidents were less likely to be lost.

It also reported:

- Priority Request-to-Talk activity down **45%** in the compared period;
- Request-to-Talk communications down **over 30%**;
- overall driver-dispatcher communications down **20%**; and
- more consistent dispatcher response times, reflected in the lower response-time standard deviation.

## Critical boundary

The report also found **little change in average RTT response time**, with the TODSS period two seconds slower in that specific comparison. This is important for Arc's research discipline in which a DSS can reduce clutter and standardize attention without automatically improving every performance measure. Arc must therefore measure each claimed benefit separately.

## Current System implication

The reusable pattern is:

**event arrives**
→ **rule evaluates**
→ **priority is assigned**
→ **relevant information is linked**
→ **staff sees the important item**
→ **staff takes action**
→ **audit history records handling**

That pattern maps naturally to Arc's existing exception queue.

---
# VII. EXISTING PRODUCTION-SYSTEMS

## 7.1 Optibus Operations

Current Optibus Operations documentation includes:

- automated rule validations;
- issue alerts;
- visual Gantt timelines;
- real-time operational adjustments;
- duty/shift changes;
- inline performance/compliance metrics; and
- integrated operations data.

Source:
- https://optibus.com/product/operations/

**Arc relevance:** supports the practicality of bringing rules, alerts, timeline views, and metrics into operational workflows.

**Boundary:** Optibus is a mature commercial platform with planning, scheduling, and operational capabilities far beyond Arc.

## 7.2 Optibus Control & Monitoring

Current Optibus Control documentation explicitly describes:

- automated alerts;
- incident/disruption monitoring;
- operational KPI views;
- logged service changes;
- reasons for changes;
- reporting; and
- using performance trends to improve future service.

Source:
- https://optibus.com/product/control-monitoring/

**Arc relevance:** supports alerting and operational-summary patterns.

**Boundary:** Control assumes live tracking and richer connected data. Arc must describe its information as latest-known application state when telemetry is absent.

## 7.3 Optibus On-Time Analytics

Optibus documents a dedicated analytics capability for visualizing and analyzing transit operational data across routes, directions, patterns, stops, trips, and time points.

Source:
- https://optibus.com/solutions/optibus-for-ontime-analytics/

**Arc relevance:** supports a separate analytics workspace rather than overloading the operational CRUD dashboard.

**Boundary:** On-time analytics commonly depends on AVL and related operational datasets that Arc does not currently possess.

## 7.4 HASTUS

Current HASTUS scheduler documentation supports:

- immediate validation feedback;
- quality/cost attributes;
- unit-availability constraints;
- alternative what-if scenarios; and
- comparison of operational alternatives.

Source:
- https://www.giro.ca/en-us/our-solutions/hastus-software/hastus-for-schedulers/

**Arc relevance:** supports future scenario analysis and immediate change feedback.

**Boundary:** HASTUS includes advanced mathematical scheduling and optimization. Arc should not reproduce that scope.

## 7.5 HASTUS/TEC workflow automation case study

GIRO's TEC case study reports that:

- 90,000 day-off requests were processed in the first year;
- for a depot with 650 drivers, processing a year's worth of day-off requests decreased from **three weeks to three days**; and
- business rules were modeled into the self-service workflow.

Source:
- https://www.giro.ca/en-us/news-insights/case-studies/tec/

**Evidence classification:** vendor-reported case study.

**Arc relevance:** provides a concrete example of deterministic workflow digitization and rule enforcement reducing administrative processing effort.

**Boundary:** this is employee self-service, not dispatch/reconciliation. The exact time savings cannot be transferred to Arc.

## 7.6 HASTUS/BART scenario generation case study

GIRO reports that BART schedulers can build a valid crewing solution in less than one hour, with weekday roster scenarios reported at 20–30 minutes and weekend scenarios at 10–15 minutes. The case study also reports a 19% reduction in pay-to-platform ratio and 24% fewer rosters with more than two hours of day-to-day start-time variation.

Source:
- https://www.giro.ca/en-us/news-insights/case-studies/bart/

**Arc relevance:** demonstrates why scenario analysis is useful in mature transit software.

**Boundary:** these results depend on optimization algorithms and BART data. They justify keeping what-if analysis as future work, not implementing an imitation optimizer in Arc.

## 7.7 RELATED SYSTEM COMPARISON

| Capability                       | FTA TODSS      | Optibus         | HASTUS          | PubtraVis / G2Viz | Arc Transit        |
| -------------------------------- | -------------- | --------------- | --------------- | ----------------- | ------------------ |
| Operational analytics            | PARTIAL        | YES             | YES             | YES               | PROPOSED           |
| Rule/constraint validation       | YES            | YES             | YES             | NOT ESTABLISHED   | PROPOSED           |
| Alert prioritization             | YES            | YES             | PARTIAL         | NOT ESTABLISHED   | PROPOSED           |
| Workflow automation              | YES            | YES             | YES             | NOT ESTABLISHED   | PROPOSED           |
| Change-impact/scenario feedback  | PARTIAL        | PARTIAL         | YES             | PARTIAL           | LIMITED PROPOSAL   |
| Data-quality dashboard           | PARTIAL        | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED   | PROPOSED           |
| Unified operational timeline     | PARTIAL        | YES/PARTIAL     | PARTIAL         | NOT ESTABLISHED   | PROPOSED           |
| Advanced optimization            | NO / not focus | YES             | YES             | NO                | OUT OF SCOPE       |
| Live AVL dependency              | YES            | COMMON          | NOT ALWAYS      | GTFS data         | NO CORE DEPENDENCY |
| Manual-first provenance emphasis | PARTIAL        | NOT ESTABLISHED | NOT ESTABLISHED | NOT ESTABLISHED   | YES                |

**Important:** `NOT ESTABLISHED` means the reviewed public material does not establish the exact capability. It does not mean the product cannot provide it.

## 7.8 CLAIMS THIS PROJECT MUST NOT MAKE

1. Analytics proves why a real-world operational event occurred.
2. The DSS makes optimal decisions.
3. The DSS is artificial intelligence.
4. Automated rules eliminate the need for dispatch staff.
5. A lower exception count means physical operations are safer.
6. A high record-health result proves real-world truth.
7. The system predicts future disruptions without a validated forecasting model.
8. The system improves public transportation generally based only on prototype tests.
9. The system reduces congestion.
10. The system reduces accidents.
11. The system lowers fuel expenditure without a deployment study.
12. The system is better than Optibus/HASTUS/TODSS-like systems.
13. Vendor-reported savings from another operator will be achieved by Arc.
14. Dashboard visualization automatically improves every decision.
15. Automation always reduces response time.
16. More alerts mean better oversight.
17. More metrics mean better decision making.
18. A correlation between fuel prices and cancellations proves causation.
19. Manual data becomes verified truth because it is audited.
20. Current PostgreSQL performance will remain sufficient at every future scale.

## 7.9 CONTRADICTORY / CAUTIONARY FINDINGS

The research contains useful cautions.

## a. TODSS did not improve every response-time measure

The Pace TODSS evaluation found a slight increase in the compared average RTT response time: 86 seconds to 88 seconds.

Therefore:

> Do not use the TODSS case to claim that automation inherently makes dispatch responses faster.

The stronger supported findings concern reduced information clutter, communication volume, and consistency.

## b. Dashboards depend on information quality

The 2024 dashboard experiment indicates that information currency and completeness matter.

Therefore:

> A visually impressive Arc analytics page built on stale or incomplete records would undermine the purpose of the feature.

## c. Transit analytics often assumes richer data than Arc has

Many current analytics systems use:

- AVL;
- ticketing;
- passenger counts;
- GPS;
- realtime feeds.

Arc does not have those in its core.

Therefore:

> Analytics must be limited to the fields Arc actually records.

## d. Vendor case-study gains are context-specific

GIRO and Optibus case studies can demonstrate what production systems and users have achieved.

They do not prove Arc will produce the same savings.


---

# VIII. FEATURES TO BE ADDED

## 8.1 DETAILED OPERATIONAL ANALYTICS

Arc already stores structured operational events. Without an analytics layer, users must inspect lists and individual records to identify longer-term patterns.

The system should help answer:

- What changed?
- How often?
- Where?
- Which routes/resources are affected most?
- What reasons are being recorded?
- Is the situation improving or worsening?
- Which records should be investigated further?

Relevant evidence includes:

- PubtraVis user testing with 122 real-world users;
- G2Viz's operational visualization approach;
- the 2024 dashboard experiment with 524 participants;
- current Optibus analytics/operations product patterns; and
- public-transport data-management research emphasizing performance measurement.

The Requirements includes a separate historical operational analytics workspace that summarizes and compares only information actually stored or derived from the application.

## a. Core Analytics

### 1. Fleet

- availability rate over time;
- unavailable unit count by reason;
- maintenance downtime;
- out-of-service duration;
- unit utilization frequency;
- fleet availability by route/service day.

### 2. Trips / dispatch

- planned trips;
- assigned trips;
- dispatched trips;
- completed trips;
- cancelled trips;
- curtailed trips;
- completion rate;
- cancellation rate;
- cancellation/curtailment reasons;
- assignment conflicts prevented;
- replacement assignments.

### 3. Incidents

- incidents by type;
- incidents by severity;
- average/median resolution duration;
- unresolved incident age;
- incidents linked to cancellations;
- incidents by route/unit.

### 4. Maintenance

- maintenance events by category;
- average downtime;
- repeat maintenance frequency;
- vehicles currently held;
- maintenance-related assignment blocks.

### 5. Reconciliation

- exceptions created;
- exceptions resolved;
- exceptions by rule;
- exception age;
- repeat exceptions by entity;
- false-positive count from controlled review if a review label exists.

## b. Analytics Hierarchy

Divided in to three hierarchies, Every chart should be traceable to underlying records.
### 1. Overview
→ organization-level KPIs

### 2. Drilldown
→ route, vehicle, driver, date range, reason, status

### 3. Evidence
→ open the exact records/events behind a number

## c. Implementation Exclusion

The folloeing features/features that are unsupported analytics are excluded unless the required data and definitions are actually present:

→ "safety score"
→ "driver quality score"
→ "accident probability"
→ "passenger satisfaction"
→ "route profitability"
→ "traffic reduction"
→ "fuel efficiency"

## d. Evaluation Metric

The following tests will be implemented to evaluate the added features:

1. time to identify the route with the highest cancellation rate;
2. time to identify the most common recorded cancellation reason;
3. time to determine which vehicle spent the longest in maintenance;
4. accuracy of answers using analytics versus raw record tables;
5. analytics page p50/p95 latency;
6. database query latency;
7. memory and CPU during aggregate queries.

---

## 8.2 OPERATIONAL RECORD HEALTH AND DATA-QUALITY MONITORING

Arc's central research problem is not only operational state; it is **the reliability and coherence of the record used to represent that state**.

A polished operational dashboard can be misleading if its underlying records are:

→ stale
→ incomplete
→ unverified
→ contradictory
→ missing provenance
→ outside valid effective periods.

The strongest research support includes:

- Philippine evidence concerning verification difficulty in manual monitoring;
- Urbano et al. (2025) on accuracy, completeness, consistency, timeliness, granularity, and information richness for public-transport decision making;
- Newmark (2024) on continuous transit-data accuracy assessment; and
- the 2026 traffic data-quality monitoring framework using explicit thresholds and alerting.

The system requirement should expose the health of the operational record separately from operational performance.

## a. Defined States of Operational Record Quality

### 1. Completeness

Examples:
→ required reason present;
→ required source present;
→ related trip present;
→ required effective timestamp present;
→ incident resolution information complete.

### 2. Timeliness / freshness

Examples:
→ active trip not updated past configured threshold;
→ manual availability report too old;
→ verification overdue.

### 3. Consistency

Examples:
→ active maintenance while vehicle marked available;
→ cancelled trip still in active projection;
→ driver unavailable while assigned.

### 4. Provenance coverage

Examples:
→ percentage of manual reports with source type;
→ percentage with reporting user;
→ percentage with effective timestamp.

### 5. Verification

Examples:
→ count/percentage of critical manual reports remaining unverified.

### 6. Effective-period validity

Examples:
→ assignments using a route version valid for the service date.

## b. Presentation of Operational Records

Recommended:

```text
OPERATIONAL RECORD HEALTH

Current records                         97.1%
Aging records                           1.9%
Stale records                           1.0%

Unverified critical reports                4
Open consistency exceptions                7
Missing provenance                          2
Invalid route-version references            0
Missing required reason codes               6
```

Avoid an opaque single number such as  Health score = 93.7% unless the formula and weights are explicitly documented. To show the individual metrics first.

## c. Defined States Limitation

Arc can assess whether its records meet configured quality rules. It cannot automatically determine whether a human-entered statement is physically true.

## d. Evaluation Metric

Seed controlled datasets with known:

→ missing fields
→ stale timestamps
→ contradictory states
→ unverified reports
→ invalid route versions.

Measure:
→ detection recall
→ precision
→ false-positive rate
→ false-negative rate
→ quality-scan duration
→ time from seeded violation to displayed alert.

---
# 8.3 TRANSPARENT LIGHTWEIGHT RULE-BASED DECISION-SUPPORT SYSTEM AND EXCEPTION PRIORITIZATION

Arc already detects contradictions.

The next step is not artificial intelligence.

The next step is to make each detected condition **operationally interpretable**.

For each important rule, Arc should be able to show:
1. observed input
2. calculation or condition
3. configured threshold
4. resulting severity
5. explanation
6. recommended review/action
7. responsible role
8. override or acknowledgment
9. audit history

Example:
```text
RULE
Route availability below configured minimum

INPUTS
Required units: 20
Available units: 13

CALCULATION
13 / 20 = 65%

THRESHOLD
HIGH if < 75%

RESULT
HIGH OPERATIONAL CONSTRAINT

SUPPORTING RECORDS
3 units in MAINTENANCE
2 units OUT_OF_SERVICE
2 units with stale availability reports

RECOMMENDED REVIEW
Review planned assignments and reserve-unit availability.

AUTHORITY
Human operations staff decides what to change.
```

The FTA TODSS evaluation directly supports:
→ incident prioritization
→ rule/threshold processing
→ action plans
→ reduced information clutter
→ standardized handling
→ audit history.

Current Optibus Operations also documents automated rule validation and issue alerts.

## a. Pre-Defined Classes of Rules

### 1. Eligibility rules

→ unavailable vehicle assigned
→ unavailable driver assigned
→ expired license
→ invalid route version
→ overlap

### 2. Freshness rules

→ stale active trip
→ aging incident
→ aging unverified report

### 3. Capacity/availability rules

→ available fleet ratio below configured threshold
→ reserve fleet below configured threshold

### 4. Data-quality rules

→ missing source
→ missing required reason
→ missing resolution link

## b. Class Prioritization

A transparent priority model may use:
→ base severity
→ operational state
→ age
→ number of affected future assignments

Example:
```text
priority = base_severity
         + age_band
         + affected_assignment_band
```

The exact formula must be
→ simple
→ configurable
→ visible to administrators
→ versioned
→ testable

## c. Evaluation Metrics

Use seeded rule scenarios

Metrics:
→ precision
→ recall
→ false-positive rate
→ false-negative rate
→ priority-order correctness
→ p50/p95 rule-evaluation latency
→ user time to identify top-priority exception
→ rate of recommendations opened / reviewed.

---

# 8.4 DETERMINISTIC WORKFLOW AUTOMATION

The most useful automation for Arc is not autonomous operational control. It is the removal of repetitive checking and clerical transitions that already have deterministic rules.

As referenced from the evidence basis, the FTA TODSS prototype automatically:
→ processed events
→ prioritized incidents
→ aged/expired incidents from queues
→ linked action plans
→ retained audit history

The TEC/HASTUS vendor case study provides an additional concrete workflow example in which rules and digital processing substantially reduced administrative processing time.

## a. Defined Automated Delegations

### 1. Automatic exception creation

When a configured contradiction becomes true:

```text
IF maintenance.status = IN_PROGRESS
AND vehicle.availability = AVAILABLE
THEN create/update exception
```

### 2. Automatic exception closure candidate

When the contradiction no longer exists:

```text
IF violation no longer true
THEN mark exception = READY_FOR_REVIEW
```

Do not silently delete history.

### 3. Freshness calculation

Compute:
→ CURRENT
→ AGING
→ STALE

from time and configured thresholds.

### 4. Scheduled scans

Examples:
→ reconciliation scan every N minutes;
→ license-expiry scan daily;
→ unresolved incident aging scan periodically.

### 5. Assignment-impact notification

When a vehicle becomes unavailable:

→ identify future active assignments;
→ create review task/alert.

### 6. Derived metric refresh

Refresh cached aggregates if caching becomes necessary.

## b. What will remain as human-controlled

Do not automatically:
→ cancel a trip;
→ reassign a driver;
→ declare a vehicle roadworthy;
→ close an incident requiring investigation;
→ approve an override;
→ modify an official route version.

## c. Automation Audit

Every automated action should record:
→ automation/rule identifier;
→ rule version;
→ trigger time;
→ affected entity;
→ prior state;
→ resulting state;
→ reason;
→ correlation ID;
→ whether human review is required.

## d. Method of Evaluation

Compare Arc baseline versus automated iteration for controlled tasks:

→ number of manual checks;
→ number of clicks;
→ task completion time;
→ missed stale cases;
→ missed expiry cases;
→ time-to-exception creation;
→ automation execution latency;
→ incorrect automation rate;
→ percentage requiring manual reversal.

---

# 8.5 CHANGE-IMPACT ANALYSIS

Arc has multiple linked operational states. A valid update in one module can create consequences elsewhere.

Examples:
 →vehicle becomes unavailable
 →driver becomes unavailable
→ maintenance starts
→ route version changes
→ trip is cancelled
→ incident affects a scheduled assignment

The normal record saved CRUD response is insufficient when the changed record is operationally connected to other records.

The system requires before or immediately after a high-impact state change, Arc should identify directly affected operational records and present them for review.

Example:
```text
CHANGE
Vehicle B-17 -> MAINTENANCE

DIRECTLY AFFECTED
3 future active assignments
2 route services
1 unresolved incident

REQUIRES REVIEW
Trip T-503 — 08:30
Trip T-509 — 11:00
Trip T-528 — 15:20
```

Route-version example:
```text
ACTIVATE ROUTE VERSION 4
Effective: 2026-11-01

18 future trip instances currently reference Version 3
12 occur after Version 4 becomes effective

ACTION
Review the 12 affected trip instances.
```

Current HASTUS documentation explicitly provides immediate validation feedback and supports evaluating operational alternatives/impacts. HASTUS planning documentation also emphasizes evaluating impacts of proposed network changes.

Arc's parent research independently establishes that:

→ route versions change
→ availability affects assignments
→ cross-module consistency matters.

Comparing to HASTUS, arc transit is  all about**dependency/impact identification**, not optimization. Arc does not automatically calculate the "best" replacement plan unless a separate validated optimization model is later developed.

## a. Method of Evaluation

Seed changes with a known dependency graph.

Metrics:
→ affected-record recall;
→ false-positive affected-record rate;
→ impact-query latency;
→ time for user to identify all affected assignments;
→ number of missed dependent records.

---

# 8.6 OPERATIONAL EVENT TIMELINE AND RECONSTRUCTION

Arc already keeps event history and audit information. Without a unified presentation, investigators or supervisors may need to open multiple modules and manually reconstruct what happened. Arc should include a presentable chronological operational timeline that combines related dispatch, availability, incident, maintenance, exception, verification, and audit events.

Example:
```text
TRIP T-402

07:40  Trip PLANNED
07:49  Vehicle B-17 ASSIGNED
08:02  Driver D-21 ASSIGNED
08:11  Trip DISPATCHED
08:46  Vehicle problem reported
08:48  Incident I-91 created
08:50  B-17 -> OUT_OF_SERVICE
08:51  Reconciliation exception EX-33 generated
08:56  Vehicle B-22 assigned as replacement
09:02  Service resumed
10:31  Trip COMPLETED
10:34  Incident RESOLVED
```

The FTA TODSS report discusses audit history for:

→ how incidents were handled;
→ incident-specific historical service disruptions; and
→ management review.

Arc's own event-history model already provides the raw data needed.

## a. Architecture Design

Prefer a query/read model such as:

`operational_timeline_view`

or a service that merges:

- `dispatch_event`;
- `vehicle_availability_event`;
- `driver_availability_event`;
- `incident_event`;
- `maintenance_event`;
- `exception_event`;
- `audit_event`.

Use:

- `correlation_id`;
- `related_event_id`;
- affected entity IDs;
- timestamps.

Avoid duplicating every event into another permanent table unless benchmark evidence shows a need.

## b. Method of Evaluation

Controlled scenario:

`problem report -> incident -> vehicle unavailable -> exception -> replacement -> completion -> resolution`

Metrics:

- causal-link completeness;
- timeline chronological correctness;
- time to reconstruct scenario using raw module screens;
- time to reconstruct using unified timeline;
- timeline query p95 latency.

---
# IX. ADDED FEATURE CAPABILITY MATRIX

| Capability                      | Evidence strength                   | Fit with Arc |           Data readiness | Implementation feasibility | Evaluation feasibility | Priority        |
| ------------------------------- | ----------------------------------- | -----------: | -----------------------: | -------------------------: | ---------------------: | --------------- |
| Detailed operational analytics  | High                                |    Very High |                Very High |                       High |              Very High | CORE            |
| Record-health monitoring        | Very High                           |    Very High |                Very High |                       High |              Very High | CORE            |
| Rule-based DSS / prioritization | High                                |    Very High |                Very High |                       High |              Very High | CORE            |
| Deterministic automation        | High                                |    Very High |                Very High |                       High |              Very High | CORE            |
| Change-impact analysis          | Moderate–High                       |    Very High |                     High |                       High |              Very High | SUPPORTING      |
| Event timeline / reconstruction | High                                |         High |                Very High |                       High |              Very High | SUPPORTING      |
| What-if scenario sandbox        | High industry relevance             |       Medium |                   Medium |                     Medium |                   High | FUTURE          |
| External contextual data        | High contextual relevance           |       Medium |               Low–Medium |                     Medium |                 Medium | FUTURE          |
| Statistical forecasting         | High general relevance              |       Medium | Low until history exists |                     Medium |                 Medium | FUTURE          |
| GTFS/API expansion              | High standard relevance             |       Medium |                   Medium |                       High |                   High | OPTIONAL/FUTURE |
| ML anomaly detection            | Insufficient Arc need               |          Low |                      Low |                     Medium |              Difficult | OUT             |
| Predictive maintenance          | High industry relevance, wrong data |          Low |                 Very Low |                        Low |                    Low | OUT             |
| Autonomous dispatch             | High complexity/risk                |          Low |                      Low |                        Low |                    Low | OUT             |
| Digital twin                    | Low fit                             |     Very Low |                 Very Low |                   Very Low |                    Low | OUT             |

## ADDED FEATURE SCORING HEURISTIC

This is a **design heuristic**, not empirical evidence.

Score each feature 0–4 for:

- evidence strength;
- direct Arc fit;
- data readiness;
- implementation feasibility;
- evaluation feasibility.

Maximum = 20.

| Feature | Evidence | Fit | Data | Feasibility | Evaluation | Total |
|---|---:|---:|---:|---:|---:|---:|
| Operational analytics | 4 | 4 | 4 | 4 | 4 | **20** |
| Record-health monitoring | 4 | 4 | 4 | 4 | 4 | **20** |
| Rule-based DSS | 4 | 4 | 4 | 4 | 4 | **20** |
| Deterministic automation | 4 | 4 | 4 | 4 | 4 | **20** |
| Change-impact analysis | 3 | 4 | 4 | 4 | 4 | **19** |
| Event timeline | 4 | 4 | 4 | 4 | 4 | **20** |
| What-if sandbox | 4 | 3 | 2 | 3 | 4 | **16** |
| External contextual data | 3 | 3 | 2 | 3 | 3 | **14** |
| Statistical forecasting | 3 | 3 | 1 | 3 | 2 | **12** |
| ML anomaly detection | 2 | 1 | 1 | 2 | 2 | **8** |
| Digital twin | 1 | 1 | 0 | 1 | 1 | **4** |

Suggested interpretation:

- 18–20: implement now;
- 15–17: supporting/future;
- 11–14: investigate first;
- 0–10: reject for this iteration.

---

# X. PROPOSED UI / MODULES

## 10.1 Analytics

Suggested route:

`/analytics`

Tabs:

- Overview
- Fleet
- Trips / Dispatch
- Incidents
- Maintenance
- Exceptions

Common filters:

- date range;
- route;
- vehicle;
- driver;
- status;
- reason;
- source type.

Every aggregate should support:

`View underlying records`

## 10.2 Operations Action Center

Suggested route:

`/action-center`

Sections:

- Critical
- High
- Medium
- Aging
- Awaiting verification
- Recently resolved

Each item shows:

- violated rule;
- observed values;
- threshold;
- affected records;
- recommended review;
- age;
- responsible role;
- history.

## 10.3 Record Health

Suggested route:

`/data-quality`

Panels:

- Freshness
- Completeness
- Verification
- Consistency
- Provenance
- Effective-period validity

## 10.4 Timeline

Possible access:

- `/trips/{id}/timeline`
- `/vehicles/{id}/timeline`
- `/incidents/{id}/timeline`

---

# XI. DATA-MODELS
## 11.1 Existing entities

Most analytics should derive from:
- `fleet_unit`;
- `vehicle_availability_event`;
- `driver_availability_event`;
- `route`;
- `route_version`;
- `trip_instance`;
- `dispatch_assignment`;
- `dispatch_event`;
- `operational_report`;
- `incident`;
- `incident_event`;
- `maintenance_record`;
- `maintenance_event`;
- `exception_case`;
- `exception_event`;
- `audit_event`.

## 11.2 Minimal new concepts

Possible:

### `rule_definition`

- id;
- code;
- name;
- category;
- severity;
- enabled;
- threshold fields where needed;
- version;
- effective_from;
- description.

Prefer code-defined logic with database-configurable thresholds initially.

Avoid a generic scripting/rules engine until there is a proven need.

### `rule_evaluation`

Only persist when useful for audit or performance study:

- rule;
- rule version;
- evaluated_at;
- entity;
- outcome;
- observed values;
- generated exception.

Do not persist every harmless evaluation if storage cost becomes unnecessary.

### `automation_execution`

- automation code;
- started_at;
- completed_at;
- status;
- affected count;
- correlation_id;
- error summary.

### `metric_snapshot`

Optional.

Only add if benchmarks show that on-demand aggregation is too slow.

## 11.3 Views/read models

Recommended:

- `analytics_trip_daily`;
- `analytics_fleet_daily`;
- `record_health_summary`;
- `operational_timeline_view`.

Start with ordinary SQL views / repository queries.

Introduce materialized views only if measured query performance requires them.

---
# XII. DEVELOPMENT TOOLS

- Java 21;
- Spring Boot;
- Spring Modulith;
- Spring Security;
- PostgreSQL;
- Flyway;
- Maven;
- Docker Compose;
- JUnit;
- Testcontainers;
- Vaadin Flow.

Possible module boundaries, that remain modules in one deployment.:

```text
arc-transit
|
|-- fleet
|-- driver
|-- route
|-- dispatch
|-- incident
|-- maintenance
|-- reporting
|-- reconciliation
|-- audit
|
|-- analytics          NEW
|-- decision-support   NEW
|-- automation         NEW
|-- data-quality       NEW
|-- impact             NEW
`-- timeline           NEW READ MODEL
```

---
# XIII. RULE DESIGN PRINCIPLES

Every DSS/automation rule should have:

- stable rule code;
- human-readable name;
- purpose;
- inputs;
- threshold/condition;
- severity;
- response;
- human owner;
- override rule;
- version;
- audit behavior;
- test cases.

Example:

| Field | Value |
|---|---|
| Code | `FLEET_AVAILABILITY_LOW` |
| Input | available units / configured required units |
| Medium | < 85% |
| High | < 75% |
| Response | Create/update action-center item |
| Automatic operational change | None |
| Human action | Review assignments/service plan |
| Audit | Rule version + observed values |

---

## 13.1 ALERT-FATIGUE CONTROL

The FTA TODSS evaluation is especially relevant here because it measured information clutter.

Arc should avoid:

- creating a new alert every scan for the same unresolved condition;
- presenting low and critical severity identically;
- keeping expired informational warnings permanently in the main queue.

Recommended:

- one active exception per rule/entity combination;
- update `last_detected_at`;
- configurable severity;
- aging rules;
- auxiliary/low-priority view;
- acknowledgment;
- resolution;
- suppression only with reason and audit.

Metrics:

- alerts per operational hour;
- duplicate-alert suppression rate;
- average active exception count;
- exception age;
- user-dismissed false-positive count.

---

## 13.2 HOW THE ANALYTICS SHOULD BE VALID/INVALID

## Valid

> 31 trips were recorded as cancelled during the selected period.

> 14 of those cancellations have `FUEL_COST` as the recorded reason.

> Recorded vehicle availability declined from 90% to 76%.

> Four critical reports remain unverified.

## Invalid without additional evidence

> Fuel prices caused all cancellations.

> The route is unsafe.

> Demand is falling.

> The operator is inefficient.

> The system predicts a breakdown.

> Service will fail next week.

---

## 13.3 WHAT THE ITERATION KNOWS, INFERS, AND CANNOT KNOW

## a. Analytics

### Knows

14 cancellation records during the selected period use reason code `FUEL_COST`.

### Can infer

Fuel-cost reasons represent a substantial share of the application's recorded cancellations for that period.

### Cannot know

Whether fuel prices were the complete real-world cause of each cancellation without stronger evidence.

### b. DSS example

### Knows

13 of 20 configured required units are recorded AVAILABLE.

### Can infer

The configured availability threshold is violated.

### Cannot know

Whether every AVAILABLE or UNAVAILABLE status is physically correct.

### c. Change-impact example

### Knows

Vehicle B-17 is referenced by three future assignments.

### Can infer

Changing B-17 to unavailable requires those assignments to be reviewed.

### Cannot know

Which replacement vehicle is operationally best unless the required decision model is defined.

---

# XIV. ARCHITECTURAL IMPLICATIONS

| Need                   | Response                                            | Why                                   | Trade-off                               |
| ---------------------- | --------------------------------------------------- | ------------------------------------- | --------------------------------------- |
| More aggregate queries | PostgreSQL indexes and optimized queries            | Existing relational data already fits | Requires query tuning                   |
| Periodic checks        | Spring scheduled jobs                               | Simple deterministic workload         | Single-process scheduler assumption     |
| Rule organization      | Dedicated domain service/module                     | Keeps logic testable                  | More application code                   |
| Historical metrics     | SQL aggregation/read model                          | Avoids new analytics infrastructure   | Large history may require caching later |
| Action-center alerts   | Existing exception model + severity/age             | Reuses core concepts                  | Must avoid alert fatigue                |
| Impact analysis        | Query dependency relationships before/after command | Directly reflects relational model    | Complex queries possible                |
| Timeline               | Read model over existing event tables               | Avoids duplicating source events      | Cross-table query complexity            |
| Performance scaling    | Benchmark before caching/materialization            | Evidence-driven                       | Optimization comes later                |

## 14.1 What cannot be added yet

- Kafka
- separate analytics microservice
- Elasticsearch
- ClickHouse
- Spark
- data lake
- Redis solely for dashboards
- Python analytics service
- ML serving infrastructure.

Any of those may become reasonable for future implementations, but only after PostgreSQL/Spring benchmarks show an actual bottleneck.

---

# XV. EVALUATION PLAN

The new iteration must be evaluated against the completed baseline Arc application. Do not compare against commercial vendors as if the prototype is a product benchmark.

## 15.1 Analytics information-retrieval test

**Research question:**  
Does the analytics page reduce the effort required to identify operational patterns compared with raw operational tables?

**Tasks:**

- identify highest cancellation route;
- identify most common cancellation reason;
- identify vehicle with longest maintenance downtime;
- identify oldest unresolved high-severity exception.

**Independent variable:**  
Interface: baseline records vs analytics interface.

**Dependent variables:**

- task completion time;
- answer accuracy;
- clicks/navigation actions.

**Do not assume result.**

## 15.2 Record-health detection test

Seed:

- 30 stale records;
- 20 missing-provenance records;
- 10 route-version violations;
- 15 unverified critical reports;
- valid control records.

Measure:

- precision;
- recall;
- false-positive rate;
- false-negative rate;
- scan duration.

## 15.3 DSS prioritization test

Create a fixed set of exceptions with a known priority ordering.

Measure:

- rule correctness;
- severity correctness;
- ordering correctness;
- user time to identify the top five required reviews.

## 15.4 Automation workload test

Compare:

**Baseline**
manual checks

vs.

**Iteration**
scheduled deterministic checks.

Tasks:

- identify stale active trips;
- identify licenses nearing expiry;
- find overdue incidents;
- identify newly invalid assignments.

Metrics:

- manual actions;
- completion time;
- missed cases;
- automatically generated correct review items.

## 15.5 Change-impact test

Seed:

- vehicle status changes;
- driver availability changes;
- route version activation;
- maintenance start.

Each test has known affected assignments.

Metrics:

- impact recall;
- impact precision;
- query latency;
- task completion time.

## 15.6 Timeline reconstruction test

Scenario:

`vehicle problem -> report -> incident -> unavailable -> exception -> replacement -> completion`

Compare:

- baseline separate module pages;
- unified operational timeline.

Metrics:

- time to reconstruct;
- missing causal links;
- incorrect event-order statements;
- timeline response latency.

## 15.7 Performance benchmark

Seed reproducible datasets.

Suggested synthetic scales:

### Small

- 30 fleet units;
- 60 drivers;
- 10 routes;
- 30 days history.

### Medium

- 250 fleet units;
- 500 drivers;
- 50 routes;
- 365 days history.

### Stress dataset

- 1,000 fleet units;
- 2,000 drivers;
- 100 routes;
- 2 years synthetic history.

These are **benchmark datasets**, not claims about a typical Philippine operator.

Measure:

- analytics overview p50/p95;
- drilldown p50/p95;
- impact-query p50/p95;
- timeline p50/p95;
- reconciliation duration;
- data-quality scan duration;
- automation cycle duration;
- PostgreSQL query latency;
- CPU;
- memory;
- database size;
- event-history growth.

## 15.8 BASIS OF EVALUATION METRICS

The iteration should produce a results table like this only **after implementation**:

| Metric | Arc baseline | Arc iteration | Difference | Interpretation |
|---|---:|---:|---:|---|
| Median pattern-identification task time | TEST | TEST | TEST | Not yet measured |
| Correct answers | TEST | TEST | TEST | Not yet measured |
| Manual workflow steps | TEST | TEST | TEST | Not yet measured |
| Exception recall | TEST | TEST | TEST | Not yet measured |
| False-positive rate | TEST | TEST | TEST | Not yet measured |
| Impact-identification time | TEST | TEST | TEST | Not yet measured |
| Timeline reconstruction time | TEST | TEST | TEST | Not yet measured |
| Analytics p95 latency | TEST | TEST | TEST | Not yet measured |
| Data-quality scan duration | TEST | TEST | TEST | Not yet measured |
| Memory usage | TEST | TEST | TEST | Not yet measured |
| DB storage growth | TEST | TEST | TEST | Not yet measured |

Never pre-fill improvement claims.

## 15.9 SOFTWARE PERFORMANCE TARGETS

Targets may be established during implementation, but they must remain labeled **TARGET** until measured.

Possible initial engineering targets:

- common analytics dashboard p95 < 1,000 ms on medium synthetic dataset;
- drilldown p95 < 1,500 ms;
- individual rule evaluation < 50 ms where executed synchronously;
- impact preview p95 < 500 ms for common resource changes;
- scheduled full reconciliation/data-health scan completes within a defined operational window;
- no unbounded memory growth across repeated analytics queries.

These are design targets only.

They are not evidence of achieved performance.

## 15.10 QA PLAN

## Unit tests

- every rule;
- every threshold boundary;
- every priority calculation;
- every freshness classification;
- every data-quality metric.

## Integration tests

Use Testcontainers PostgreSQL for:

- aggregate queries;
- event joins;
- impact analysis;
- scheduled jobs;
- concurrent state changes;
- audit persistence.

## Property / invariant tests

Examples:

- unavailable resources cannot be newly assigned;
- one active exception per rule/entity combination;
- resolved exceptions remain historically visible;
- analytics totals reconcile with source records;
- route-version analytics never silently mix invalid effective periods.

## Security tests

- analytics follows RBAC;
- sensitive driver data excluded from aggregate exports;
- rule configuration restricted to authorized roles;
- audit cannot be edited through normal application workflow.

## 15.11 RISK ANALYSIS

| Failure mode                 | Cause                                | Consequence                  | Mitigation                                |
| ---------------------------- | ------------------------------------ | ---------------------------- | ----------------------------------------- |
| Misleading metric            | Wrong denominator/filter             | Bad interpretation           | Metric definition + tests + drill-through |
| Stale dashboard              | Cached result not refreshed          | User trusts old summary      | Display calculated-at time                |
| Alert flood                  | Poor thresholds                      | Staff ignores alerts         | Deduplication, severity, tuning           |
| Wrong prioritization         | Bad weight/threshold                 | Important case deprioritized | Simple rules + tests + override           |
| Automation loop              | Job repeatedly recreates same case   | Clutter                      | Idempotency key / active-case rule        |
| Impact false negative        | Missing relationship/query           | Dependent assignment missed  | Controlled dependency tests               |
| Timeline missing event       | Correlation link absent              | Incomplete reconstruction    | Completeness checks                       |
| Sensitive analytics exposure | Aggregate/detail permission mismatch | Privacy breach               | Server-side authorization                 |
| Metric gaming                | Users optimize number not outcome    | Distorted behavior           | Multiple metrics + audit + review         |

---

# XV. FUTURE ADDITIONS

## 15.1 Fuel prices

The Philippine Department of Energy maintains weekly Metro Manila and regional pump-price information for gasoline, diesel, and kerosene.

Source:
- https://doe.gov.ph/prices/fuels

Potential future use:

- contextual panel next to internally recorded fuel-related disruptions;
- historical comparison.

Boundary:

Do not infer causation solely from temporal coincidence.

## 15.2 Weather / hazards

Potential future data:

- PAGASA rainfall/flood warnings;
- severe weather advisories;
- local road-closure information.

Before implementation determine:

- authoritative source;
- API/feed availability;
- geographic resolution;
- update frequency;
- retention;
- source provenance.

## 15.3 Regulatory changes

Possible:

- fare orders;
- route changes;
- relevant LTFRB notices.

Keep external context separate from internal operational truth.

## 15.4 Data Forecasting

Do not add forecasting merely because sufficient chart history exists.

Before forecasting:

1. define the target variable;
2. confirm enough historical observations;
3. inspect missingness and structural breaks;
4. define a baseline;
5. compare models;
6. use time-aware validation;
7. report error;
8. communicate uncertainty;
9. prove the forecast changes a useful decision.

Potential simple baselines:

- previous period;
- moving average;
- seasonal average;
- linear trend.

Only use more advanced models when they outperform a simple baseline meaningfully.

Forecasting remains **FUTURE**.

## 15.5 OPTIONAL ADDITIONS

## a. GTFS AND APIs

GTFS remains the appropriate standard if Arc later publishes standardized transit information.

Official documentation distinguishes:

- GTFS Schedule;
- GTFS Realtime trip updates;
- service alerts;
- vehicle positions; and
- trip modifications.

Source:
- https://gtfs.org/documentation/overview/

Arc should not publish fake "realtime" feeds from stale manual records.

GTFS Schedule export remains the safest earlier interoperability step.

---
# XVI. APPENDIX
# 16.1 DEVELOPMENT SEQUENCE

## Iteration 2A — Analytics foundation

Implement:

- metric definitions;
- date/route filters;
- trip/fleet/incident/maintenance analytics;
- drill-through to records.

Evaluate before moving on.

## Iteration 2B — Record health

Implement:

- completeness metrics;
- freshness metrics;
- verification metrics;
- provenance coverage;
- consistency metrics.

## Iteration 2C — Action center / rule-based DSS

Upgrade current exceptions with:

- explanations;
- observed values;
- threshold;
- severity;
- recommendation;
- aging;
- acknowledgment.

## Iteration 2D — Deterministic automation

Add:

- scheduled scans;
- idempotent exception generation;
- expiry/aging warnings;
- review tasks.

## Iteration 2E — Change impact

Add preview for:

- vehicle availability;
- driver availability;
- maintenance start;
- route version activation.

## Iteration 2F — Timeline

Add unified read model and event reconstruction UI.

## Gate

Do not add future features until the six modules above have:

- correctness tests;
- performance results;
- usability/task evaluation;
- documented failure modes.

---

## KEYWORDS

Use the following search phrases for future research:

## Analytics

- `public transport operational analytics`
- `transit visual analytics`
- `public transport performance dashboard`
- `bus operations analytics`
- `transit data decision making`

## Decision support

- `Transit Operations Decision Support System TODSS`
- `dispatcher decision support public transport`
- `transit incident prioritization`
- `rule based transit operations decision support`

## Data quality

- `public transport data quality completeness timeliness`
- `GTFS accuracy metrics`
- `traffic data quality monitoring thresholds`
- `operational data quality dashboard`

## Automation

- `transit workflow automation dispatch`
- `automated rule validation transit operations`
- `dispatcher alerts incident prioritization`

## Change impact

- `public transport schedule change impact`
- `transit what-if scenario`
- `vehicle availability scheduling impact`
- `route change impact analysis`

## Timeline / reconstruction

- `transit incident audit history`
- `operations event timeline`
- `dispatch event history incident reconstruction`

---
# 45. REFERENCES

## Philippine government and public-sector context

Department of Energy. (2026). *Weekly fuel prices & oil monitor*.  
https://doe.gov.ph/prices/fuels

Department of Transportation / Republic of the Philippines. (2025). *General Appropriations Act provisions relevant to the Public Transport Modernization Program* [copy hosted by LTFRB].  
https://ltfrb.gov.ph/wp-content/uploads/2025/01/DOTR_GAA-2025.pdf

Philippine Information Agency. (2023, June 19). *MMDA's Bus Management and Dispatch System now monitors EDSA Busway*.  
https://mirror.pia.gov.ph/news/2023/06/19/mmdas-bus-management-and-dispatch-system-now-monitors-edsa-busway

Philippine Information Agency. (2025, August 14). *Davao City Interim Bus System garage taking shape*.  
https://pia.gov.ph/news/davao-city-interim-bus-system-garage-taking-shape/

Philippine Information Agency. (2026, April 14). *Marcos Jr. gov't launches P1-B service contracting April 15*.  
https://pia.gov.ph/news/marcos-jr-govt-launches-p1-b-service-contracting-april-15/

Philippine Information Agency. (2026, April 15). *Service Contracting Program? Here's what you need to know*.  
https://pia.gov.ph/news/service-contracting-program-heres-what-you-need-to-know/

Philippine News Agency. (2026, May 20). *Lipa City's AI traffic system aims to cut peak-hour travel by 15-20%*.  
https://www.pna.gov.ph/articles/1275408

Presidential Communications Office. (2026, April 14). *Malacañang press briefing ... DOE, DOTr, LTFRB, LANDBANK and DOF*.  
https://pco.gov.ph/press-briefing/malacanang-press-briefing-of-palace-press-officer-and-pco-undersecretary-claire-castro-with-department-of-energy-doe-secretary-sharon-garin-department-of-transportation-dotr-acting-secretary-giov/

## Government technical evaluation

Hiller, W., & Luck, K. (2009). *Transit Operations Decision Support System (TODSS) Core Requirements Evaluation and Update Recommendations*. U.S. Federal Transit Administration.  
https://rosap.ntl.bts.gov/view/dot/4373  
PDF:  
https://www.transit.dot.gov/sites/fta.dot.gov/files/docs/Technical_Memorandum_TODSS_Core_Requirements_Evaluation_and_Update_Recommendations_Final_Report_v4_%28final%29_%282%29.pdf

Hiller, W. (2010). *Transit Operations Decision Support System (TODSS) Core Requirements Prototype Development Case Study and Lessons Learned*. U.S. Federal Transit Administration.  
https://rosap.ntl.bts.gov/view/dot/4374

## Peer-reviewed and institutional research

Hjelle, S., Mikalef, P., Altwaijry, N., & Parida, V. (2024). *Organizational decision making and analytics: An experimental study on dashboard visualizations*. Information & Management, 61(6), 104011.  
https://doi.org/10.1016/j.im.2024.104011

Newmark, G. L. (2024). *Assessing GTFS Accuracy*. Mineta Transportation Institute.  
https://transweb.sjsu.edu/research/2017-Public-Transit-Statistical-Analysis

Para, S., Wirotsasithon, T., Jundee, T., Demissie, M. G., Sekimoto, Y., Biljecki, F., & Phithakkitnukoon, S. (2024). *G2Viz: an online tool for visualizing and analyzing a public transit system from GTFS data*. Public Transport.  
https://doi.org/10.1007/s12469-024-00362-x

Prommaharaj, P., Phithakkitnukoon, S., Demissie, M. G., Kattan, L., & Ratti, C. (2020). *Visualizing public transit system operation with GTFS data: A case study of Calgary, Canada*. Heliyon, 6(4), e03729.  
https://pmc.ncbi.nlm.nih.gov/articles/PMC7160583/

Urbano, V. M., Arena, M., & Azzone, G. (2025). *Big data for decision-making in public transport management: A comparison of different data sources*. Research in Transportation Business & Management, 59, 101298.  
https://doi.org/10.1016/j.rtbm.2025.101298

*Data Quality in Traffic Management: Framework and Real-World Impacts*. (2026). Future Transportation, 6(3), 124.  
https://doi.org/10.3390/futuretransp6030124

## Existing production systems

GIRO. *HASTUS for schedulers*.  
https://www.giro.ca/en-us/our-solutions/hastus-software/hastus-for-schedulers/

GIRO. *TEC case study*.  
https://www.giro.ca/en-us/news-insights/case-studies/tec/

GIRO. *BART case study*.  
https://www.giro.ca/en-us/news-insights/case-studies/bart/

Optibus. *Operations*.  
https://optibus.com/product/operations/

Optibus. *Control & Monitoring*.  
https://optibus.com/product/control-monitoring/

Optibus. *On-Time Analytics*.  
https://optibus.com/solutions/optibus-for-ontime-analytics/

## Standards

General Transit Feed Specification. *Overview*.  
https://gtfs.org/documentation/overview/


---
# PS. remove this SECTION after the project has been COMPLETED
 
 INTEGRATION NOTE FOR [[research]]

Do **not** merge this file into [[research]] yet.

When the extension has been reviewed, the safest integration points in the parent document would be:

1. add a short "Iteration 2 / Post-Core Enhancements" section after the existing feature-priority section;
2. move only accepted capabilities into the parent feature list;
3. retain advanced/future features under future scope;
4. extend the traceability matrix with the additional evidence;
5. add the new controlled experiments to the evaluation plan;
6. preserve the parent project's main title and core problem statement unless the extension changes the actual research objective.

The parent system should remain recognizable as Arc Transit even after these additions.



# WHAT SHOULD I DO RIGHT NOW, AS RECOMMENDED

## IMPLEMENT NOW

1. **Detailed operational analytics**
2. **Operational record/data-quality monitoring**
3. **Transparent rule-based DSS / exception prioritization**
4. **Deterministic workflow automation**

These four features are the strongest combination of:

- direct relevance;
- data readiness;
- implementation feasibility;
- evidence support; and
- measurable value.

## IMPLEMENT AFTER CORE

5. **Change-impact analysis**
6. **Operational event timeline/reconstruction**

They strongly improve usability of the existing state/history architecture but do not need to block the first iteration.

## INVESTIGATE FURTHER / FUTURE

- what-if scenario analysis;
- DOE fuel-price context;
- weather/hazard context;
- regulatory/fare context;
- statistical forecasting;
- GTFS Schedule export;
- external API.

## DO NOT IMPLEMENT IN THIS ITERATION

- LLM assistant;
- AI chatbot;
- ML anomaly detector;
- predictive maintenance;
- autonomous dispatch;
- city traffic optimization;
- digital twin;
- proprietary telemetry stack;
- microservices/Kafka without measured need.


# 44. SOURCE AUDIT TABLE

| Source | Main use | Type | Date | Evidence role | Boundary |
|---|---|---|---|---|---|
| PIA — fuel shock / reduced runs | Parent Philippine service-change context | Government information | 2026-04-14 | Current direct / case-program context | Does not prove chronic nationwide condition |
| PIA — Service Contracting monitoring | Manual + GPS monitoring | Government information | 2026-04-15 | Current direct | Specific program |
| PCO press briefing | Verification difficulty of manual monitoring | Official transcript | 2026-04-14 | Current direct | Specific program/official statement |
| PIA — MMDA BMDS | Existing digital dispatch | Government information | 2023-06-19 | Current within parent window | EDSA Busway context |
| DOTr GAA/PTMP dashboard provision | Dashboard/monitoring policy context | Official budget provision | 2024-12-30 / FY2025 | Policy context | Public program dashboard, not private operations |
| PNA — Lipa SEERMO | Current PH analytics/DSS context | Government news agency | 2026-05-20 | Current case-specific | Stated target, not achieved result |
| PIA — Davao Interim Bus System | Current OCC/fleet monitoring context | Government information | 2025-08-14 | Current case-specific | Different infrastructure |
| FTA Pace TODSS evaluation | Rules, priority, clutter, automation, audit, quantified outcomes | Government technical evaluation | 2009 | Historical architectural/evaluation | CAD/AVL context; old |
| Urbano et al. | Transit decision-making data quality | Peer-reviewed research | 2025 | Current international research | Lombardy operators |
| Hjelle et al. | Dashboard information quality / decision-making | Peer-reviewed experiment | 2024 | Current general IS research | Not transport-specific |
| Prommaharaj et al. | Transit visualization/usability | Peer-reviewed research | 2020 | International architectural evidence | GTFS-based |
| G2Viz | Transit visualization/analysis | Peer-reviewed research | 2024 | Current international architectural evidence | GTFS-based |
| Newmark / MTI | Continuous transit data accuracy metrics | Transport research institute | 2024 | Current data-quality evidence | GTFS-specific |
| Future Transportation DQ framework | DQ thresholds / alerting | Peer-reviewed research | 2026 | Current architectural evidence | Traffic management, not Arc |
| Optibus Operations | Rules, alerts, metrics | Official product docs | Retrieved 2026-10-05 | Industry capability | Vendor documentation |
| Optibus Control | Alerts, logs, KPIs, trends | Official product docs | Retrieved 2026-10-05 | Industry capability | Rich live-data assumptions |
| Optibus Analytics | Dedicated analytics | Official product docs | Retrieved 2026-10-05 | Industry capability | AVL/data-rich assumptions |
| HASTUS Schedulers | validation / what-if | Official product docs | Retrieved 2026-10-05 | Industry capability | Optimization suite |
| GIRO TEC | workflow automation reported outcome | Vendor case study | Retrieved 2026-10-05 | Reported implementation result | Not independent; different workflow |
| GIRO BART | scenarios / reported productivity | Vendor case study | Retrieved 2026-10-05 | Reported implementation result | Not transferable to Arc |
| DOE fuel prices | possible future contextual data | Philippine official source | Current 2026 page | Future data source | Must not infer causality |
| GTFS | interoperability | Open standard docs | Current | Standard | Realtime requires appropriate live source |

---

# 47. FINAL DESIGN PRINCIPLE

> **Iteration 2 should not make Arc appear more intelligent. It should make Arc's existing operational record more useful, more interpretable, easier to monitor, easier to act on, and easier to evaluate.**

The preferred chain is:

**record**
→ **quality check**
→ **metric**
→ **rule**
→ **priority**
→ **explanation**
→ **human action**
→ **audit**
→ **measured result**

rather than:

**record**
→ **opaque prediction**
→ **unverifiable recommendation**