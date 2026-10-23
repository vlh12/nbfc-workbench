# Version 3: clean database and reviewed workflow

Implemented at the user's request on 22 October 2026. This request supersedes the earlier rule that automatically marked warning-free contributor inputs APPROVED. Financial formulas, configured threshold classification, saved loan amounts, four business services, and the final scenario assessment decision rules are unchanged.

## 1. Database cleanup

The version-3 backend processes were stopped before cleanup. A local, restorable JDBC backup was written before deletion. Cleared 78 records: 28 saved portfolio/supporting inputs; 5 stress runs; 6 assessments; 1 follow-up; 19 audit events; 16 legacy exposure projection rows; and one each processed-event marker, outbox event and exposure batch. Legacy report tables had zero rows. Accounts, login settings/sessions, seed reference catalogue, database tables and Flyway migration histories were preserved. No Oracle system objects were removed.

Only exposure-analytics-v1's exposure-batch-validated offset was moved to the topic end after its consumer stopped. Retained Kafka messages and other consumer groups were not deleted. This prevents old exposure events from repopulating the cleared projection on restart.

Recovery backup lives in the Codex task output/backups/before-workflow-reset-20261022 folder. Its counts.txt records the exact table counts. ResetDemo.java beside the task work files provides a restore mode, requiring empty target run tables; it does not restore account credentials. Stop services before any deliberate recovery. This is a local demo recovery copy, not a full Oracle instance backup.

## 2. Dataset

Use samples/workflow-demo/README.txt. Three portfolios (July, August, September 2026) each have 16 loans and eight NBFCs. All include explicit EAD, INR/RUPEE, valid reporting/revenue dates and reconciled principal/interest/repayment/outstanding amounts. There are three matching capital entries and 24 asset-quality entries (eight counterparties per period). Each quality entry has seven mutually exclusive buckets reconciling to its own total. NBFC borrower quality is not inferred from bank loan DPD.

Named threshold policies use MEDIUM 5/HIGH 10 and an optional comparison MEDIUM 4/HIGH 8. They remain synthetic demonstration settings, not regulatory limits. Seven named scenario plans include exact expected results and cover baseline/moderate/severe, changed recovery, changed threshold policy and cross-period comparison. Negative examples cover duplicate-loan rejection, missing EAD and reconciliation warnings. Supporting-data CSVs are form-entry guides, not portfolio-upload files.

The website's Complete workflow CSV link downloads the September portfolio. No dataset was automatically loaded for normal use after final cleanup. Optional PowerShell 7 importer prompts for an existing Contributor login and local verification code. Default mode validates only; -Import saves all portfolio/support inputs as pending. It never approves data or analyst assessments. The former full-demo importer delegates to this authenticated flow.

## 3. Meaningful names

Portfolio names are entered by the contributor. Capital labels include the reporting period and supplied capital definition. Asset quality is labelled by counterparty and reporting date. Thresholds display their saved policy name. Analysts enter a scenario name (up to 120 characters) separately from the rationale. That name follows the saved run into history, comparison, assessment review and the PDF. Older runs have a descriptive NBFC/severity/recovery fallback. UUID keys remain stable internally and appear only where technical traceability needs them.

## 4. Initial Supervisor data review

Contributor: upload -> Validate -> explicit Save. A saved portfolio, capital or quality input has PENDING_REVIEW status. Validation warnings remain visible and never become a silent approval.

Supervisor: Review & Follow-up -> 1. Review contributor data. Select the named input. Inspect loan values, capital definition/date or quality buckets/total. Enter a review reason. Approve each required input or return it with correction instructions. Approval of one input does not approve the others. Missing EAD or portfolio reconciliation warnings block approval. The original submitted payload is retained; returned data requires a new corrected version. A submitter cannot approve their own data. Concurrent decisions use optimistic locking, and a completed decision cannot be overwritten.

Contributor: Supporting data -> Submitted data and Supervisor feedback shows all saved inputs and recorded approval/return notes. A returned portfolio also shows its correction note under Saved portfolios.

Analyst: the API lists only approved inputs and rejects direct reads of pending/returned input IDs. Stress execution forwards the actual signed-in session to Access & Data, so approved portfolio/capital/quality access is enforced in the calculation path. Dates, bank, NBFC quality scope and explicit EAD remain checked. Supervisor-created thresholds remain versioned configurations, not contributor submissions.

Implementation: Access migration V9 adds a separate append-only portfolio_data_review table and a lock version on portfolio_document. The submitted payload is not rewritten. GET /api/portfolios/{id}/reviews and POST /api/portfolios/{id}/review supply feedback and authorized decisions.

## 5. Analyst calculation and final Supervisor decision

Analyst selects an approved portfolio, matching capital, same-NBFC quality and the named threshold policy. Enter the scenario name, severity, recovery and rationale. Calculate/save, check the result, add the assessment note and submit. Assessments reads /api/governance/my-assessments, restricted to the signed-in analyst; it no longer calls the forbidden Supervisor-wide queue.

Supervisor: Review & Follow-up -> 2. Review analyst scenarios & follow-up. Review the saved calculation, approve or return with a reason, and manage follow-ups through work/evidence/verification/closure. The result's LOW/MEDIUM/HIGH classification does not change when a human approves it.

## 6. Final PDF

Downloads & Reports remains locked until the latest completed scenario for the selected portfolio has its newest assessment APPROVED with a recorded decision actor/date. Status is polled and rechecked at download. A newer pending/returned submission cannot borrow an older approval. A failed/missing approval lookup blocks generation. The PDF includes only that approved scenario, portfolio charts, preview visualizations, initial portfolio/capital/quality decisions and notes, analyst submission, final Supervisor reason/date, and recorded follow-up history. JSON/CSV remain separate data exports. Export Report in the toolbar now opens this approval-gated Downloads screen instead of calling an unverified export directly.

## 7. Verification and boundaries

All 140 backend tests passed, including new data review, self-review, warning rejection, preserved payload, authorization and analyst account-scoping cases. Frontend typecheck and production build passed; the existing large-bundle warning remains non-blocking. Live authenticated checks exercised all three roles, 30 initial data approvals, seven exact scenario calculations, analyst submission, pending PDF lock, final approval, evidence verification/closure and PDF download. Live browser checks also passed for the heatmap, three-period NPA history, prior/current portfolio comparison, baseline/moderate/severe scenario comparison and analyst assessment status. Browser screenshots and the test PDF were inspected locally. Temporary test accounts and generated verification records are removed after checking, leaving user accounts and an empty run database.

Local reference basis: current version-3 source, context documents and docs/phase0-implementation.md. The original client Q&A remains unavailable; no new regulatory thresholds or claimed client requirements were inferred. This is the verified local prototype workflow. Production tenant identity, exclusive queue claims/lead override, deployment hardening and broader asynchronous lifecycle remain outside this change; they are not presented as completed features.
