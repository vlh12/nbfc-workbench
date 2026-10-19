# Phase 0 portfolio workflow implementation

Updated 18 October 2026. This guide covers the multi-NBFC portfolio path added alongside the existing saved lending-report and legacy batch paths.

## Source and decisions

The local Phase 0 team-review proposal was reviewed in place. Its functional requirements and worked formula informed this implementation. The original document was not edited, copied into the repository, or transmitted to an external service.

The user confirmed adding explicit EAD and configuring capital/threshold inputs without defaults. The original CSV has no EAD and includes outstanding/payable reconciliation differences. These facts are exposed as warnings; input values are never silently changed.

## Contributor to analyst to supervisor

1. Start Oracle and Kafka, build the project, and select **All workbench services** in VS Code Run and Debug. The original **Upload-to-dashboard backend** compound is retained.
2. Start the frontend in its own terminal with `npm.cmd run dev`.
3. Select Contributor and use **Portfolio risk workbench → Upload loan portfolio**.
4. Upload the supplied 19-column file, confirm synthetic data, and validate. It contains 16 loans across eight NBFCs.
5. Save the portfolio. The same immutable snapshot is available when switching to Analyst or Supervisor. These views refresh saved metadata every five seconds and select a version explicitly.
6. Review the exact reported loan amounts, DPD/status and reconciliation warnings. Selected versions are not added together. Prior uploads are retained.
7. For stress analysis, add explicit `ead_amount` to each row using the new template and correct reconciliation differences against the authoritative synthetic source. Do not derive EAD automatically from lending, outstanding or revenue.
8. As Contributor, save a same-date eligible capital version and NBFC asset-quality aggregates. Supply all seven mutually exclusive synthetic buckets and their total in INR rupees.
9. As Supervisor, save named MEDIUM and HIGH capital-impact thresholds. These are prototype settings, not regulatory limits.
10. As Analyst, select the portfolio, NBFC, capital, asset-quality and threshold versions. Enter severity, recovery and rationale. Run and save the scenario.
11. Review loss, capital impact, concentration versus labelled eligible capital, inputs, formula, threshold version and alert. Missing/invalid/stale inputs produce a stored FAILED run with no final alert.
12. Submit a completed run as an assessment. As Supervisor, select it in the review panel, inspect the saved scenario evidence, and approve or return with a reason.
13. Create follow-up actions and progress through in-progress, evidence submission, verification/reopening and closure. Legacy complete/reopen actions remain compatible.

## Contracts and services

| Owner | API | Purpose |
|---|---|---|
| Access & Data, 8081 | POST /api/portfolios/validate | Validate 19/20-column CSV; retain failed-submission metadata |
| Access & Data | POST /api/portfolios | Explicit save; multipart CSV or JSON loan rows |
| Access & Data | GET /api/portfolios | Recent bank-scoped version metadata |
| Access & Data | GET /api/portfolios/{id} | Immutable saved portfolio |
| Access & Data | POST /api/portfolios/capital | Save positive, labelled eligible capital |
| Access & Data | GET /api/portfolios/capital/{id} | Read a capital version |
| Access & Data | POST /api/portfolios/quality | Validate/save NBFC aggregate quality data |
| Access & Data | GET /api/portfolios/quality/{id} | Read a quality version |
| Access & Data | POST /api/portfolios/thresholds | Supervisor saves immutable prototype threshold version |
| Access & Data | GET /api/portfolios/thresholds/{id} | Read a threshold version |
| Exposure Analytics, 8082 | GET /api/exposures/portfolios/{id} | Exact portfolio/NBFC aggregates and labelled EAD share |
| Stress Simulation, 8083 | POST /api/scenarios | Deterministic persisted stress run with idempotency key |
| Stress Simulation | GET /api/scenarios | Recent runs for the demo bank |
| Stress Simulation | GET /api/scenarios/{id} | Immutable run inputs/results or failure |
| Risk Governance, 8084 | POST /api/governance/scenario-assessments | Submit completed run with immutable review evidence |
| Risk Governance | Existing assessments/follow-ups/audit APIs | Decisions, evidence, follow-ups and audit |

The gateway on 8090 now routes these APIs. New service-to-service calls use owner APIs rather than cross-service database reads. This portfolio slice is synchronous and does not require Kafka delivery; the older batch/Kafka path is preserved.

## Data acceptance

- UTF-8, optional BOM, exact 19-column header or that header followed by ead_amount.
- Maximum 2 MiB and 10,000 rows; field-level errors with physical line numbers.
- One demo bank and reporting date per file, multiple NBFCs, unique loan IDs.
- Nonnegative money with at most two decimals; positive amount lent.
- Positive explicit aggregate EAD is required for a stressed NBFC.
- Valid dates, revenue period ordering, INR/RUPEE, known loan status values.
- Missing EAD or reported-outstanding reconciliation differences retain the portfolio as VALIDATED for review. No stress scenario can use it as approved input.
- A saved version without warnings is APPROVED automatically after explicit Save. This implements the previously requested automatic data acceptance goal; it is distinct from supervisor assessment approval.
- JSON field names match the PortfolioData.Loan record. It is subject to the same validation as CSV.
- Invalid file submissions store VALIDATION_FAILED metadata and errors, without saving usable loan rows.

## Asset-quality and revenue interpretation

Bank-loan DPD/status is displayed as reported. It is not treated as the NBFC's own borrower-portfolio quality. Contributors submit that separate aggregate context explicitly.

Quality inputs require CURRENT, DPD_1_30, DPD_31_60, DPD_61_90, NPA_90_PLUS, RESTRUCTURED and WRITE_OFF_RECOVERY. This prototype contract uses mutually exclusive synthetic amounts and verifies the declared total. Current/prior comparison requires the same NBFC and a strictly earlier reporting date. It shows absolute INR differences. It does not infer stress severity from bucket movements.

CSV revenue values remain per-row reported values. They are not summed and presented as company-wide revenue because the supplied rows have different revenue amounts for the same NBFC/period.

## Stress and review invariants

Direct loss uses explicit EAD, severity and recovery. Capital impact divides unrounded direct loss by the selected positive eligible capital. The capital label and every input version are retained. Money is rounded HALF_UP to two decimals for output; displayed ratios use six decimals.

Severity/recovery are bounded from zero to 100. No capital/threshold defaults are populated. Missing capital, invalid thresholds, unavailable source records, unapproved portfolios, wrong NBFC or mismatched reporting dates fail safely. There is no stale-data override in this slice.

A caller-supplied idempotency key is scoped by bank and creator. Reuse with identical request content returns the saved run; different inputs under the same key return conflict. Runs are immutable. Revised assumptions create a new run ID.

Scenario assessments snapshot completed-run evidence. The creator cannot self-approve. Supervisor notes are required. Optimistic version checks protect concurrent assessment decisions. Unauthorized governance role attempts are rejected and audited.

## New migrations and compatibility

- Access & Data V5: portfolio_document, holding immutable typed JSON documents in Oracle CLOBs.
- Stress Simulation V1: stress_run, with unique scoped idempotency key.
- Risk Governance V2: scenario evidence and optimistic assessment version.
- Risk Governance V3: follow-up evidence, version and expanded allowed states.
- Risk Governance V4: tables for pre-existing legacy governance entity mappings, required for schema validation on a fresh schema.

Earlier applied migrations are preserved. Deploy service and migration changes together. Returning to an older binary after the expanded follow-up lifecycle requires reviewing those status values; no destructive rollback is supplied.

## Requirement coverage and remaining limits

| Proposal requirement | Implemented behavior / remaining qualification |
|---|---|
| FR-01 to FR-03 | CSV/JSON portfolios, JSON capital/quality, field errors and invalid-submission metadata. Demo identifiers are syntax/type checked; no production institution registry. |
| FR-04 to FR-06 | Selected-version NBFC totals, explicit labelled denominator, current/prior quality comparisons. Validation warnings remain visible and block stress approval. |
| FR-07 to FR-09 | One-NBFC configurable stress, deterministic direct loss/capital impact, safe failed runs. |
| FR-10 | Immutable threshold records, explainable LOW/MEDIUM/HIGH alerts with exact source version IDs. |
| FR-11 | Analyst submission, supervisor approval/return, self-review rejection and audit of unauthorized governance attempts. |

This is a local synthetic prototype. Selectable roles do not provide trusted identity or real bank isolation. Shared queue claiming, lead override, production audit retention/tamper controls, active-version replacement pointers and stale-data overrides still need agreed mechanics. The scenario engine stores final COMPLETED/FAILED runs rather than exposing a full asynchronous DRAFT/RUNNING/CLOSED scenario lifecycle. Assessment review and follow-up states are separate. Do not describe all of these targets as completed.

Oracle migrations and the complete live browser journey remain unverified until an isolated Oracle schema is authorized. Automated approval review rejected creating that schema because it involved a SYSTEM-account user/grant operation. Existing running services were not stopped or overwritten. Unit/controller tests and frontend build checks are separate from this outstanding integration check.

## Contributor navigation

The contributor starts on **Upload portfolio**, with a choose-file, validate, and save workflow. Saving opens **Saved portfolios** automatically. **Supporting data** contains separate Bank capital and NBFC asset quality forms. Previous reference-report and simple-exposure uploads remain under **Other upload formats**, collapsed initially. Switching between contributor views preserves the chosen file and entered supporting data. The browser regression script is scripts/check-contributor-ui.cjs; its API responses are synthetic test fixtures, not live database verification.

## Dashboard indicators and charts (19 October 2026)

User-approved additions: five analyst indicators (selected portfolio total explicit EAD, largest NBFC share, top-three share, selected completed scenario loss, capital impact), three supervisor indicators (submitted/pending, returned, overdue follow-ups), and three doughnuts (EAD distribution, current NBFC asset-quality mix, assessment status). These also remain readable by the supervisor alongside the portfolio workbench.

Concentration uses complete explicit EAD in the selected immutable portfolio. The chart groups the five largest NBFCs and Others; top-three share uses up to three largest counterparties. Missing EAD suppresses percentages; zero EAD has no distribution. Amounts and displayed percentages use integer arithmetic; chart geometry alone uses approximate numbers. Exact amounts, percentages and category labels are available in the chart table and tooltip. Selected scenario KPIs require a COMPLETED run belonging to the selected portfolio, and retain that run's capital denominator and results. They do not sum historical scenarios.

The asset-quality doughnut appears after View asset-quality trend and uses the current snapshot's seven separate buckets, reporting date and version. Optional prior-period amounts remain in the comparison table. It is separate from bank loan DPD.

Governance counts cover the records returned by existing endpoints: latest 100 assessments, and up to 100 follow-ups ordered by due date. They are not whole-database totals. Pending means SUBMITTED. Overdue means due strictly before the viewer's local calendar date, excluding COMPLETED and CLOSED; VERIFIED still awaits closure. Empty data is distinct from unavailable service data. No approval transitions, stress formulas, regulatory thresholds, API contracts or database schema were changed.

Validation: `node scripts/check-dashboard-ui.cjs` (configure PLAYWRIGHT_MODULE_PATH and UI_CHROME_PATH if needed) covers exact money arithmetic, concentration, grouped Others, scenario scope/failures, missing/zero EAD, overdue date boundaries, governance failures and mobile chart layout using synthetic mocked API responses. It does not prove an end-to-end database workflow.

## Exploratory heatmap and NPA line chart (19 October 2026)

User-approved: severity/recovery preview grid at 0, 20, 40, 60, 80 and 100 percent on each axis, and an NPA percentage history for one NBFC. Available to analyst and supervisor. No new financial model, threshold default or approval transition was introduced.

Heatmap: select a saved portfolio, then NBFC, matching-date eligible capital and threshold versions in Severity–recovery heatmap. Generate heatmap preview reads saved input versions and produces 36 cells locally. Requires an APPROVED portfolio with explicit EAD for every loan, positive selected-NBFC EAD and eligible capital, the same bank and matching capital date. Severity is the row axis; recovery is the column axis. Each cell shows capital impact at six decimals and the configured alert band. Hover/focus/click displays loss and assumptions. Integer arithmetic follows the existing phase0-direct-loss-v1 rounding: loss HALF_UP to paise; raw-loss capital impact rounded to 12 decimals for classification, then to six for display. These are previews, with no scenario/alert persistence. Saving still requires the existing scenario form, matching asset-quality context and rationale.

NPA history: load an asset-quality context with View asset-quality trend, then click Load NPA history. It reads approved QUALITY versions from the existing latest-100 input listing in batches of eight, filters bank and NBFC, selects the latest approved createdAt per reporting date (ID breaks exact ties), and sorts reporting dates ascending. NPA percentage is NPA_90_PLUS / totalAmount * 100. The line is drawn only with two distinct dates having positive totals. Zero totals are gaps, never zero-percent substitutes; exact source amounts and version IDs appear in the table and hover tooltip. Input-list limits are visible. A failed version request blocks the history display instead of presenting an incomplete trend as complete. No new records are seeded or written.

Regression command: `node scripts/check-risk-exploration-ui.cjs` with the same Playwright/Edge environment variables as the dashboard check. Uses synthetic mocked API fixtures, checks the existing worked formula, boundary alerts, small unrounded-loss percentages, no POST requests, invalid preview inputs, latest-period selection (including nanosecond timestamps), NBFC separation, zero denominators, single-period state, load errors and mobile chart bounds. Database integration is not exercised by this browser check.

## NBFC analysis, period/scenario comparisons and explanations (19 October 2026)

User-approved four additions are grouped in Analysis desk, below Saved portfolios for Analyst and Supervisor. The desk resets when the selected immutable portfolio changes. Existing upload, stress and governance workflows remain available. No database schema, saved records, model or approval transitions were changed.

- NBFC analysis: select one NBFC from the selected portfolio. The page filters its loan table, explicit EAD, count and portfolio share; missing EAD is never inferred. Portfolio-share denominator requires complete portfolio EAD and a positive total. View NBFC asset-quality history opens the existing same-bank/NBFC history reader and line chart. Saved stress results are filtered to that NBFC AND portfolio, including failed runs and their recorded reasons. Empty and unavailable services are distinct.
- Period comparison: select exact prior/current saved portfolio versions from loaded metadata. Both must belong to the same bank and prior date must be strictly earlier. The table unions NBFCs across snapshots and shows EAD, absolute/percentage EAD change and reported outstanding change. Newly listed and absent NBFCs are labelled explicitly; absence is zero only within the selected-snapshot comparison and is not evidence of closure. A zero prior EAD has no percentage-growth denominator. Missing EAD makes that NBFC's EAD delta unavailable. Unapproved/warning-bearing snapshots remain reviewable with a visible warning; this does not grant stress approval.
- Saved scenario comparison: assign Baseline, Moderate and Severe display labels to distinct COMPLETED runs for the same bank and NBFC. Baseline plus at least one other run is required. Labels are analyst selections, not persisted scenario classifications or automatically selected severity bands. Compare recorded severity/recovery, EAD, capital definition, direct loss, impact, alert, rationale, model and portfolio/capital/asset-quality/threshold versions. Differing or unrecorded versions are flagged. Reporting dates unavailable in the loaded metadata are labelled as such, not guessed.
- Risk-driver explanation: each selected comparison is described against baseline using recorded amounts and percentage-point changes. It states which severity, recovery, EAD and capital values changed or stayed equal. A single changed direct-loss input is distinguished from multiple changed inputs; differing/missing model/input evidence prevents single-driver attribution. Capital is identified as the impact denominator rather than a loss driver. Different thresholds are flagged as affecting alert comparability. No LLM service, external source or new formula is used.

Scope: the existing endpoints return the latest 100 mixed portfolio input entries and latest 100 scenario runs. The screens explicitly describe this loaded-record limit; these are not complete-history queries or an active-version pointer. Comparisons are read-only and never create scenarios, alerts or approvals. Runtime service errors clear period results and suppress misleading scenario comparisons.

Validation command: `node scripts/check-analysis-ui.cjs` using the same PLAYWRIGHT_MODULE_PATH / UI_CHROME_PATH setup as the other browser checks. Synthetic fixtures test exact monetary changes, zero/missing EAD, newly listed/absent NBFCs, date/bank guards, loan isolation, history, all comparison columns, different versions, single/multiple changed drivers, incomplete evidence, service failures, mobile widths and no writes. Browser checks mock APIs and do not establish Oracle integration.

The full synthetic demo pack is tracked in [samples/full-demo](../samples/full-demo/README.txt). Open its data-entry-guide.html for manual input values or run Import-Demo.ps1 with PowerShell 7. Default mode validates only; -Import appends synthetic records through local demo APIs. No live import checkpoint is included.
