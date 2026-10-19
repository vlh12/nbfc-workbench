# Reusable master implementation prompt

Copy the prompt below into a local agent session with repository access. Complete the task fields; do not paste restricted original documents into an externally hosted prompt. This prompt supports later implementation tasks and does not itself authorize implementation during workspace creation.

---

You are implementing the NBFC Exposure Stress & Alert Workbench in its existing repository. Deliver one tested vertical slice at a time, preserving working application behavior.

Task: <describe the requested outcome>
Requirement IDs: <REQ IDs or newly approved requirement>
Authorized scope: <services/UI/data contract touched>
Acceptance outcome: <observable user result>
Additional human decisions since context was written: <decision and evidence, or none>

1. Establish evidence and baseline. Read root AGENTS.md, .agent/AGENT.md, context/PROJECT.md, REQUIREMENTS.md, DECISIONS.md, ARCHITECTURE.md, USER-FLOWS.md, DATA-CONTRACTS.md and OPS.md as relevant. Inspect current working tree, active code paths, tests, manifests and migration history. Preserve unrelated edits. Identify which source claims are directly verified, approved goals, architect recommendations or unresolved. Existing docs/ verification claims are historical, not this run's results.

2. Reconcile sources locally. If Phase 0 proposal/client Q&A are available, read them in place, preserve synced sources and record minimal source filename/section/page references. Do not upload originals, excerpts, embeddings, credentials or internal data to external search, AI services, connectors, public CI or repositories. If unavailable, state the gap and do independent work; never reconstruct unread assistant replies or declare source analysis complete. Use only synthetic data, even in tests and screenshots.

3. Map present behavior before designing. Four business services: Access & Data, Exposure Analytics, Stress Simulation (currently scaffold), Risk Governance. Shared/Gateway are supporting modules. Preserve Java/Spring Boot, Kafka, Oracle AI Database 26ai target, React/TypeScript and existing dependency declarations. Inspect rather than assume actual installed/running versions. Do not introduce product AI/LLM dependencies. Follow the current controller -> service -> repository and matching UI; similarly named unused classes do not prove features.

4. Preserve both existing flows. Legacy /api/batches accepts bank_id,nbfc_id,exposure_amount and projects additive batches through Kafka exposure-batch-validated. Saved lending reports accept fourteen columns, validate against references, explicitly save, preserve Report IDs and expose paginated analyst/supervisor reads with exact figures/graphs. Neither accepts EAD. Do not silently substitute EAD for exposure/outstanding, add history to current totals, change migration V1-V4, or erase existing APIs/UI. Preserve Upload-to-dashboard backend startup and current STS instructions.

5. Resolve the next slice's real blockers. Explicit ead_amount and INR rupees are approved; candidate EAD-v1 columns/limits, snapshot scope, zero/date policy, activation transitions and event schema are not finalized. Snapshot history, automatic dataset approval, bank RBAC, shared supervisor review, lead override and audit are approved goals with implementation gaps. Formulas, regulatory definitions, capital/concentration denominators, thresholds, rounding, materiality and retention cannot be invented. Present concrete options/impact for the choice blocking dependent code; continue independent authorized work. Do not ask again for choices already approved by the human. Record new decisions with evidence.

6. Propose a short concrete implementation plan. State user outcome, affected modules/contracts, migration/compatibility strategy, acceptance tests, likely risks and a narrow rollback boundary. Prefer adding the smallest complete service-to-UI path over generating broad scaffolding. Existing four service boundaries are constraints; no architectural rewrite or dependency upgrade without a reason and reviewed choice. Separate Access and Data logically inside their existing service.

7. Implement only the authorized increment. For schema work, finalize the contract before application edits, then add compatible parsing, domain/persistence/event/API/UI support and fixtures together. Use explicit EAD BigDecimal amounts in INR rupees and decimal-string JSON; chart scaling is presentation. Never default missing financial inputs to zero, coerce invalid precision or calculate unapproved risk scores. Write new Flyway migrations, preserve applied history and specify rollback/compatibility limits.

8. Apply relevant lifecycle invariants once mechanics are agreed. Versioned full replacement retains prior immutable versions; activation is atomic per resolved bank scope. Recommend canonical duplicate detection, contributor difference confirmation, correction/re-upload and version-aware outbox/projections; these mechanics need decisions before implementation. Automatic dataset approval must not create a supervisor dataset gate or silently remove the current Save report action. Pending event delivery is separate from approval. Deduplicate delivery and prevent older events from rolling projections backward; do not add historical snapshots to active totals.

9. Apply access/governance rules. Trusted server identity determines bank scope; client bank_id/role headers cannot authorize production access. Preserve fail-closed demo-disabled behavior. Enforce tenant and role checks on reads, writes, queues, exports/events and object lookup. Test at least two banks and distinct actors. Keep analyst assessment approval separate from automatic dataset approval. Shared queue needs reviewed claim/concurrency mechanics; lead override needs trusted entitlement, reason, superseded decision and audit. No caller-controlled production lead flag. Preserve separation of duties and existing follow-up/audit behavior.

10. Test meaningful outcomes. Choose relevant cases from evals/acceptance-tests.md, financial-test-cases.md and failure-scenarios.md. Mark each as observed regression, target pending decision, or recommended behavior. Include decimal boundaries, scope isolation, invalid-file atomicity, stale/concurrent updates, idempotency and lifecycle audit as applicable. Use an independent hand-calculated expected result, not the implementation itself. Do not create tests that assert speculative rules as approved requirements.

11. Verify with discovered tools. Read manifests and check executable/runtime versions first; commands in OPS.md are manifest-backed candidates, not guaranteed execution. For product edits run focused checks, existing Maven tests, frontend typecheck and production build. Inspect Oracle integration profile/tests and connection variables before isolated synthetic Oracle runs; verify Kafka integration and UI flows separately when relevant. Do not invent npm test or assume a broker/database is running. If a check is blocked, record command/purpose, sanitized error and unverified outcome; continue unrelated checks. No pass claim from code inspection, historic logs or mocks alone.

12. Finish the increment. Review diff and confirm unrelated files/source originals untouched. Update decision/requirements delivery status, repository memory and dated log with minimal nonsecret evidence. Report outcome, changed behavior, executed checks/results, remaining gaps and next smallest slice. Never call a scaffold complete. Do not push, publish, deploy, message people or transmit source materials unless separately authorized. Stop once this increment's acceptance outcome is met; do not expand into speculative features.

Suggested sequence, adapted to the requested task and approved choices:
- Baseline/reconcile sources and contracts without breaking existing flows.
- Finalize EAD CSV and validation; add compatible EAD persistence/API/UI with tests.
- Implement reviewed identity/bank isolation foundation before enabling multibank workflows.
- Implement scoped versioned ingestion/automatic approval and correction/diff mechanics.
- Implement version-aware analytics/projection and replay/recovery checks.
- Implement approved stress model and explainable alerts when formulas/parameters are resolved.
- Extend assessment review with scoped claim concurrency, lead override and audit.
- Complete end-to-end synthetic contributor -> analyst -> supervisor checks and operations documentation.

This sequence is an architect recommendation. Select only the next authorized slice; missing financial-model decisions need not block unrelated preservation or access work.

Return a concise change report: requirement IDs and outcome; files/behavior changed; checks actually run; blocked/unverified items; decisions needed; next slice. Include no secrets or restricted source text.
