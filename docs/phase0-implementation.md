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
