# Decision register

As of 2026-10-16. PROJECT.md defines evidence labels. Never promote recommendations by copying them into prompts/memory.

| ID | Status | Decision / unresolved choice | Evidence and next action |
|---|---|---|---|
| DEC-01 | USER-APPROVED | Explicit ead_amount and INR rupees | U1/U2; no implicit EAD derivation. |
| DEC-02 | RECOMMENDED / UNRESOLVED | Seven-column EAD-v1, key, zero policy, limits/order/transport | DATA-CONTRACTS.md candidate; review before implementation. |
| DEC-03 | USER-APPROVED goal / UNRESOLVED mechanics | Immutable snapshot full replacement/history | Choose bank/date vs bank/NBFC/date, revisions vs periods, deletions, active pointer and additive-batch migration. |
| DEC-04 | USER-APPROVED goal / UNRESOLVED mechanics | Automatic dataset approval | Agree acceptance trigger, difference confirmation, activation/events. Preserve explicit Save report meanwhile. |
| DEC-05 | USER-APPROVED goal / UNRESOLVED mechanics | Bank RBAC/separation of duties | Choose trusted identity/bank claims, multi-role policy, service auth. Demo headers insufficient. Keycloak artifacts do not approve reinstatement. |
| DEC-06 | USER-APPROVED goal / UNRESOLVED mechanics | Shared queue/lead override | Agree ownership/expiry/release, lead entitlement, override powers and reasons/audit. Current listing is not claim control. |
| DEC-07 | UNRESOLVED | Stress model, severity/recovery/PD/LGD, denominators, rounding, thresholds/alerts | Reconcile S1/S2. No formula or regulatory threshold approved by accessible evidence. |
| DEC-08 | USER-APPROVED | Four business services and requested stack | U1. Preserve declarations; running Oracle 26ai unverified. |
| DEC-09 | UNRESOLVED | Runtime versions, pinned Oracle image, physical schemas, Kafka operations, auth provider | Inspect runtime and agree infrastructure; no silent upgrades. |
| DEC-10 | RECOMMENDED | Duplicate rejection, difference confirmation, correction/re-upload, atomic activation/outbox | Historical notes are not source verification. Review materiality/hash semantics. |
| DEC-11 | USER-APPROVED goal / UNRESOLVED mechanics | Audit | Agree completeness, retention, access, tamper-evidence and correction/override events. |
| DEC-12 | UNRESOLVED evidence | Phase 0/Q&A missing; assistant replies unreadable | Reconcile local originals before dependent financial/state-machine work. No external upload. |
| DEC-13 | USER-APPROVED | Workspace addition preserving code; reusable master prompt | U1. No product edits/startup/deployment/publication required. |
| DEC-14 | UNRESOLVED | Stress/alert integration with saved reports vs batches | Map Report ID, snapshot version and run ID explicitly before integration. |

Later entries: date, question/options, source section/page, human decision/approver, requirement/test IDs, delivery status and rollback impact. Test expectations alone do not approve business rules.

## Implementation decisions on 18 October 2026

The user answered yes to adding explicit ead_amount and accepted configurable capital/threshold inputs without invented defaults. Original 19-column portfolios remain reviewable with warnings; they cannot support stress approval without explicit EAD and resolved reconciliation. A new compatible portfolio path preserves saved lending reports. Explicit Save is retained; warning-free portfolio versions are accepted automatically. Prototype requests are scoped to the fixed demo bank; this is not production bank identity. Reviewable snapshots are immutable and explicitly selected, without an active replacement pointer. Stale dates are blocked; no override entitlement has been invented. Details: docs/phase0-implementation.md.


## User-requested reviewed workflow, 22 October 2026

The current user explicitly requested initial Supervisor confirmation of contributor data before analyst calculations. This supersedes automatic approval of warning-free new contributor inputs. Contributor save creates PENDING_REVIEW; authorized Supervisor approval/return is recorded separately, with analyst access blocked until approval. Final scenario assessment approval remains separate and is required for PDF export. Details and verification: [workflow handover](../docs/workflow-reset-and-handover.md).
