# Architecture: observed and intended

## Observed repository

| Business service | Module | Configured port | Current responsibility |
|---|---|---:|---|
| Access & Data | backend/access-data | 8081 | Batch ingestion/outbox; references, report validation/save/history/reviewer reads. Keep access/data logically separate. |
| Exposure Analytics | backend/exposure-analytics | 8082 | Kafka consumer and additive projection; no approved regulatory concentration definition. |
| Stress Simulation | backend/stress-simulation | 8083 | Spring scaffold; no verified business stress engine. |
| Risk Governance | backend/risk-governance | 8084 | Separate assessment, approve/return, follow-ups and audit prototype. |

Supporting modules: backend/shared (event/security/decimals); backend/gateway (Spring Cloud Gateway MVC, 8090). Frontend is React/TypeScript/Vite (development port 5173). Ports are configuration observations, not running-service evidence.

Legacy path: CSV -> Gateway /api/batches -> Access & Data batch/outbox transaction -> Kafka exposure-batch-validated -> Exposure Analytics summary. Event IDs deduplicate repeated delivery; distinct uploads remain additive.

Saved-report path: CSV + referenceId + synthetic -> /api/lending-reports/validate -> explicit /api/lending-reports save -> Oracle report/loan rows -> /api/lending-reports/review. Kafka projection is not needed to review saved reports.

Gateway routes /api/batches, /api/lending-references, /api/lending-reports and /api/exposures. No governance route. Governance remains separate on 8084. Preserve VS Code Upload-to-dashboard backend compound (Access & Data, Exposure Analytics, Gateway) and current STS instructions.

## Declared versions (inspected, not executed)

| Component | Declaration/source |
|---|---|
| Java target | 21, root pom.xml; installed compiler may differ |
| Spring Boot / Cloud | 3.5.7 / 2025.0.0, pom.xml |
| React / TypeScript / Vite | 19.1.1 / 5.9.2 / 7.1.7, frontend/package.json |
| Kafka container | apache/kafka:4.0.0, compose.yml; not a live broker version |
| Oracle container | container-registry.oracle.com/database/free:latest, compose.yml; floating tag does not prove 26ai |
| Oracle target | Oracle AI Database 26ai, U1; verify server release/edition locally |

Preserve declared versions/lockfiles. Verify executable availability separately. The database product name does not authorize AI dependencies.

## Target recommendations

Access & Data owns snapshot/version lifecycle; Exposure Analytics owns version-specific projections; Stress Simulation owns reviewed scenario execution; Risk Governance owns assessment/review/override/audit. Use service-owned tables/migrations and APIs/events across boundaries. Physical schema topology unresolved. No direct cross-service table reads.

Recommend transactional outbox/idempotent consumers. Existing ExposureEvent has eventId, batchId and rows(bankId, nbfcId, exposureAmount). Proposed activation events need schema version, event ID, bank/scope, snapshot/version and correlation/causation metadata; name, payload transport, ordering and compatibility need review. ExposureBatchApproved is a historical proposal, not an existing contract.

An active pointer selects one immutable version per resolved scope; projections replace that scope rather than adding history. Preserve legacy compatibility until migration is agreed. No rewrite to a monolith, new platform or replacement UI is authorized.

## Gaps

ApiSecurity accepts caller-controlled X-Demo-Role in demo mode for CONTRIBUTOR/ANALYST/SUPERVISOR. No trusted bank claims, lead role or production authentication established. Bank scope must come from trusted server identity, not uploaded bank_id or arbitrary headers. Enforce scope on every read/write/list/event/override and preserve demo-disabled fail-closed behavior.

Governance queue lists recent assessments; it has no demonstrated claim lock. Active controller/service paths do not establish bank scoping, lead override or complete separation of duties. Follow controller -> service -> repository; unused similarly named entities do not prove features.

Existing configuration has credential literals. Never copy values into agent files/logs/external systems. Cleanup/rotation is a prerequisite to publication and outside this documentation-only change.
