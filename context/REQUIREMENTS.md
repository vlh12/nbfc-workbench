# Requirements and delivery status

Use evidence labels from [PROJECT.md](PROJECT.md). U1/U2 are human evidence; S1/S2 remain unavailable. Source-specific acceptance and unspecified behavior need reconciliation before implementation.

| ID | Requirement | Evidence/status | Observed implementation or gap |
|---|---|---|---|
| REQ-01 | Synthetic-only examples, fixtures and uploads; no real financial/personal data | USER-APPROVED U1 | Guards exist; prefix/checkbox alone cannot prove origin. |
| REQ-02 | Four business services; Java/Spring Boot, Kafka, Oracle AI Database 26ai, React/TypeScript | USER-APPROVED U1 | Modules exist; Stress Simulation is scaffold; runtime unverified. |
| REQ-03 | Explicit ead_amount per exposure; all money INR rupees | USER-APPROVED U1/U2 | Neither current CSV accepts EAD. Candidate in DATA-CONTRACTS.md needs finalization. |
| REQ-04 | Full snapshot replacement retaining immutable history | USER-APPROVED U1 | Batches are additive; saved reports preserve submissions but have no active-snapshot lifecycle. Scope unresolved DEC-03. |
| REQ-05 | Automatic dataset approval after successful validation | USER-APPROVED U1 | No approved-dataset state machine established. Does not remove explicit Save report. Trigger unresolved DEC-04. |
| REQ-06 | Bank-scoped RBAC and separation of duties | USER-APPROVED U1 | Demo roles exist; saved reports use fixed SYN-BANK-001. Production identity/isolation gaps. |
| REQ-07 | Supervisor shared queue and lead override | USER-APPROVED U1 | Listing/decisions exist; exclusive claim, lead entitlement, concurrency and override policy gaps. |
| REQ-08 | Audit material lifecycle/governance actions | USER-APPROVED U1 | Governance audit exists; completeness, tenant scope and tamper protection unverified. |
| REQ-09 | Preserve application code in this workspace task | USER-APPROVED U1 | Only agent guidance/fixtures/checker added; README/AGENTS extended. |
| REQ-10 | Finalize exposure schema/validation before implementation | USER-APPROVED U2 | Candidate schema remains RECOMMENDED; do not call it finalized. |

## Architect recommendations pending review

REC-01: reject duplicate snapshot submissions with scoped canonical content hashing; event idempotency is separate.
REC-02: show material differences and obtain contributor confirmation before activating replacement. Materiality metric/threshold unresolved. This introduces no manual approver.
REC-03: support in-app correction and complete-file re-upload, rerun validation, retain lineage, never edit active immutable versions.
REC-04: atomically persist version, active pointer, audit and outbox; make consumers version-aware/idempotent. Review database/event compatibility first.
REC-05: bind analytics to immutable dataset/scenario/model versions and explain alerts. Formulas, denominators, thresholds and notifications need source reconciliation/approval.
REC-06: prevent self-review, atomically claim queue items, require lead entitlement/reason for override, preserve prior decisions. Entitlement/transitions unresolved.

Historical planning also suggested these mechanics, but inaccessible documents and unread assistant replies are not proof of client approval. Ask only about unresolved choices affecting the next increment; continue independent authorized work.

## Preserve existing behavior

Contributor selects bank reference, uploads one NBFC's complete loan set for a reporting date, validates, explicitly saves and inspects history. Analyst/Supervisor sees paginated saved reports, selected Report ID details, exact rupee loan figures, grouped chart controls and revenue once per report. A saved report is not automatically an active exposure snapshot. Legacy /api/batches and governance APIs remain separate.

Do not infer growth from unequal/overlapping revenue periods or sum repeat submissions as extra lending. Preserve decimal-string monetary APIs and BigDecimal calculations. Chart scaling to crore is presentation only: 1 crore = 10,000,000 rupees.

## Current increment on 18 October 2026

The supplied Phase 0 proposal has now been reviewed. Contributor multi-NBFC upload, version-selected reviewer analytics, configurable capital/quality/threshold inputs, deterministic stress, immutable scenario evidence and expanded follow-up actions have been implemented. Original no-EAD CSV data is preserved and labelled. Production identity, active-pointer replacement, exclusive queue claims/lead override and full asynchronous scenario lifecycle remain gaps. Live Oracle/browser verification is still pending; see docs/phase0-implementation.md.
