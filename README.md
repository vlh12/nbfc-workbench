# NBFC Exposure Stress & Alert Workbench

## Approved technical direction

- Frontend: React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, Recharts.
- Backend: Java 21, compatible Spring Boot and Spring Cloud Gateway, Maven.
- Local synthetic demo roles with Spring Security endpoint rules. No real login or identity provider.
- Events: Apache Kafka.
- Persistence: Oracle AI Database 26ai, Spring Data JPA, Oracle JDBC.
- Migrations: Flyway, pending selected-version compatibility and licensing verification.
- Local infrastructure: Docker Compose.
- Verification: JUnit, Mockito, Testcontainers.

Exact dependency versions remain to be selected and verified.

## Approved service boundaries

1. Access & Data: logically separate access and data modules; ingestion, validation, corrections and batch tracking.
2. Exposure Analytics: consolidated exposures, concentration calculations, risk indicators and dashboard data.
3. Stress Simulation: scenario versions, stressed losses, capital impact and completion events.
4. Risk Governance: thresholds, alerts, assessments, supervisor decisions, follow-up actions and audit history.

The gateway and identity provider are supporting infrastructure.

## Requirements status

The supplied architecture diagram is an available reference. The Phase 0 proposal, client Q&A and agreed workflows have not yet been inspected. Do not invent formulas, thresholds, upload fields, permissions or approval transitions. Supervisor dataset approval is marked proposed in the diagram and remains unresolved.

## Delivery constraints

Use synthetic data only. Keep synced files under the parent project's sources directory read-only. The user has authorized building the upload-to-dashboard slice. Further business workflows require source-document review and step-by-step discussion.

## First slice source

Implemented source covers selectable synthetic demo roles, strict CSV validation, batch and durable outbox persistence, Kafka delivery, idempotent analytics projection and a role-protected exposure summary. Stress Simulation and Risk Governance contain service scaffolds only.

The user requested removal of Keycloak. Use [the current local demo instructions](docs/local-demo.md). Demo roles are freely selectable and do not provide authentication.

See [local run instructions](docs/runbook.md), [architecture](docs/architecture.md) and [requirements status](docs/requirements-status.md).

## Verification

For the user's existing native Oracle account, see [local Oracle settings](docs/oracle-local.md). Both local services now use the user-supplied account, with separate migration history tables.

Builds and tests have not run because the host shell and Node runtime fail to start. Git initialization is pending. Dependency versions, container startup and the complete browser workflow still need execution checks. Do not treat this source as a verified release.
