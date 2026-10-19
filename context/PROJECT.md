# Project and evidence map

Local inspection: 2026-10-16, Asia/Kolkata. Internal project guidance; synthetic examples only. Restricted source documents must stay local.

Build on this repository. Four business services exist: Access & Data, Exposure Analytics, Stress Simulation and Risk Governance. Gateway and shared are supporting modules, not additional business services. Current saved lending-report and older batch flows coexist. Preserve both while implementing reviewed increments.

## Evidence labels

- **USER-APPROVED**: explicit human instruction in the current request or a readable human message in the referenced conversation. Approval of a goal does not finalize unspecified formulas, schemas or state transitions.
- **OBSERVED**: inspected repository behavior/configuration; code inspection is not an executed test.
- **RECOMMENDED**: architect proposal for review, not a client requirement.
- **UNRESOLVED**: missing evidence or a choice requiring agreement before dependent implementation.
- **HISTORICAL**: earlier notes or reports; reverify before relying on them.

These labels are independent of delivery status: an approved requirement can remain unimplemented.

## Source register

| ID | Evidence | Availability and use |
|---|---|---|
| U1 | Current workspace request, 2026-10-16 | Approves this workspace and names target stack, EAD, INR rupees, snapshots, automatic approval, bank RBAC, supervisor queue, lead override and audit. Does not authorize application changes in this task. |
| U2 | Build System Architecture, conversation 6ac86b62-c464-8322-a442-4881dfa6e386 | Read through read_thread. Human chose ead_amount per exposure and requested schema/validation finalization before implementation. Assistant replies are content-reference placeholders; full schemas/decisions unavailable. An attached lending-report-demo.csv is named, but conversation attachment content was not read. |
| S1 | Uploaded Phase 0 proposal | UNAVAILABLE: parent sources/ has zero entries. No direct verification or quotations possible. |
| S2 | Uploaded client Q&A | UNAVAILABLE for the same reason. Do not attribute requirements or numeric rules to this document. |
| R1 | pom.xml; frontend/package.json; compose.yml | Declared build dependencies and infrastructure definitions. Not proof of installed/running versions. |
| R2 | CsvValidator.java, UploadService.java, ExposureEvent.java, ExposureConsumer.java | Legacy three-column batch contract, transactional outbox and additive projection. |
| R3 | LendingReportCsvValidator.java, LendingReportValidationService.java, LendingReportService.java, LendingReportController.java | Fourteen-column saved reports, validate then explicit save, bank reference checks and reviewer reads. |
| R4 | GovernanceController.java and GovernanceService.java | Assessment decisions, follow-ups and audit. Queue route lists recent assessments; exclusive claiming/lead override not established. |
| R5 | ApiSecurity.java; gateway application.yml; .vscode/launch.json | Demo roles and actual gateway/startup wiring. |
| H1 | docs/requirements-status.md; docs/architecture.md | Older batch-oriented descriptions. Some statements superseded by code/current README. Test-pass statements are historical claims. |
| H2 | Existing root README.md | Saved-report handover and outstanding checks. Historical verification claims are not newly verified results. |

R2-R5 are under backend service Java/resource trees; locate exact packages using the existing README source-location table and context/ARCHITECTURE.md module map.

## Reconcile missing sources

When S1/S2 become locally available, read them in place without editing synced sources. Record local filename, section/page, requirement ID, interpretation, conflicts and resolution in DECISIONS.md. Preserve any source confidentiality classification without inventing one. Store minimal derived guidance, not verbatim restricted documents. Do not upload source documents, excerpts, embeddings, credentials or internal datasets to search, external AI, connectors, public CI or other external services. Use local extraction tools. Missing evidence blocks only work dependent on that evidence.

Current targets are grounded in U1/U2. S1/S2 analysis remains pending and must never be represented as complete.

## Phase 0 reconciliation on 18 October 2026

The user supplied the local Phase 0 team-review proposal and a 19-column, 16-loan CSV. Both were read locally. S1 is now available for this task; S2 client Q&A remains unavailable. See docs/phase0-implementation.md for derived requirement coverage and verification limits. No restricted original was copied into the repository.
