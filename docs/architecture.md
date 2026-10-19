# NBFC Exposure Stress & Alert Workbench architecture

> Current portfolio workflow (18 October 2026): see [Phase 0 implementation](phase0-implementation.md) for the 19/20-column upload, all-service startup, analytics, stress/review APIs, and outstanding verification limits. The older descriptions below cover the earlier upload/report slice and are superseded where the new guide differs.

This project is a synthetic-data prototype for collecting NBFC exposure records, calculating exposure totals, submitting analyst assessments and recording supervisor decisions. It uses a React frontend, Spring Boot microservices, Oracle Database, Kafka and a Spring Cloud Gateway.

## Business services

| Service | Port | Responsibility | Main interfaces and data |
|---|---:|---|---|
| Access & Data | 8081 | Accepts contributor CSV uploads, validates rows, stores batch metadata and publishes validated batches. | `POST /api/batches`, `GET /api/batches`; `exposure_batch`, `exposure_record`, `outbox_event` |
| Exposure Analytics | 8082 | Consumes validated-batch events, creates an idempotent exposure projection and serves totals grouped by NBFC. | `GET /api/exposures/summary`; projection tables; Kafka consumer |
| Stress Simulation | 8083 | Reserved boundary for scenario execution, capital impact and loss calculations. The current implementation is a health-check scaffold. | No business endpoint yet |
| Risk Governance | 8084 | Stores analyst assessments, supervisor decisions, follow-up actions and audit events. | `/api/governance/*`; governance tables |

The shared module contains common event models, decimal JSON serialization and the local demo security filter. The Gateway is a Spring Cloud Gateway MVC application on port 8090. The original upload-to-dashboard gateway routes `/api/batches/**` to Access & Data and `/api/exposures/**` to Exposure Analytics. Risk Governance can run directly on port 8084; it is not included in the original upload compound.

## Upload-to-dashboard flow

~~~text
Contributor browser
    -> React frontend :5173
    -> Gateway :8090
    -> Access & Data :8081
    -> Oracle batch + outbox transaction
    -> Kafka topic: exposure-batch-validated
    -> Exposure Analytics :8082
    -> Oracle exposure projection
    -> Analyst/Supervisor summary
~~~

Access & Data validates UTF-8 CSV files with `bank_id`, `nbfc_id` and `exposure_amount`. Invalid input is rejected atomically with line-level errors. A valid upload creates a new batch and an outbox event in one database transaction. The publisher sends the event to Kafka and marks it published only after Kafka acknowledges it.

Exposure Analytics consumes the event and records the event ID before committing normalized rows and totals. This prevents duplicate Kafka delivery from double-counting exposure. Re-uploading the same file intentionally creates a new additive batch.

## Risk Governance flow

An Analyst submits a validated batch for review. A Supervisor can approve it or return it for correction, create follow-up actions, complete or reopen actions and inspect the append-only audit history. Assessment states are `SUBMITTED`, `RETURNED` and `APPROVED`; follow-up states are `OPEN` and `COMPLETED`.

The governance API is role-protected and runs on port 8084. Its gateway route is intentionally absent in the original upload-to-dashboard configuration; add a `/api/governance/**` route when governance is promoted into the main gateway deployment.

## Data ownership

Each service owns its tables and migrations. Access & Data owns ingestion records and the outbox. Exposure Analytics owns its projection and processed-event marker. Risk Governance owns assessment, follow-up and audit records. Services communicate through HTTP APIs and Kafka events instead of reading another service's tables.

## Runtime and security

Oracle Free/AI Database provides persistence and Kafka provides asynchronous delivery. Docker Compose starts Oracle, Kafka, services and frontend. Native VS Code debugging starts the original three-service upload compound: Access & Data, Exposure Analytics and Gateway.

Keycloak was removed at the user's request. Local demo roles are selected in the UI and sent as `X-Demo-Role`. This is a development mechanism, not authentication or user isolation. Production needs an identity provider, validated tokens, service audiences, rate limits, secrets management and audit controls.

## Current limits

The HLD marks dataset approval as proposed. Stress formulas, regulatory concentration definitions, scenario calculations, risk thresholds and alert generation require the unavailable Phase 0 proposal and client Q&A. Stress Simulation therefore remains a scaffold, and the current analytics total is an aggregation rather than a regulatory risk judgement.

