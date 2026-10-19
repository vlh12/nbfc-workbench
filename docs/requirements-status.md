# Requirements and verification status

> Current portfolio workflow (18 October 2026): see [Phase 0 implementation](phase0-implementation.md) for the 19/20-column upload, all-service startup, analytics, stress/review APIs, and outstanding verification limits. The older descriptions below cover the earlier upload/report slice and are superseded where the new guide differs.

The approved stack is React 19, TypeScript, Vite, Tailwind, Recharts, Java 21, Spring Boot, Spring Cloud Gateway MVC, Kafka, Oracle Database and JPA/Flyway. The project is organized as Access & Data, Exposure Analytics, Stress Simulation and Risk Governance, with logical data ownership between services.

## Implemented

- Synthetic CSV upload with `bank_id`, `nbfc_id` and `exposure_amount`.
- File size, header, identifier and decimal validation with line-level errors.
- Atomic batch and outbox persistence.
- Kafka `exposure-batch-validated` publication with at-least-once handling.
- Idempotent Exposure Analytics projection and NBFC summary dashboard.
- Analyst assessment submission.
- Supervisor assessment approval/return, follow-up actions and audit history.
- Local demo role authorization for Contributor, Analyst and Supervisor.
- Docker Compose definitions for Oracle, Kafka, backend services and frontend.
- VS Code configuration for the original `Upload-to-dashboard backend` compound.

## Explicit prototype assumptions

- IDs begin with `SYN-`; amounts are non-negative INR values with up to two decimal places.
- The synthetic-data checkbox and identifier prefix are demo guards, not proof of data origin.
- A validated batch is an ingestion result, not an approval or delivery status.
- Exposure totals are aggregation by NBFC, not regulatory concentration or risk judgement.
- Re-uploading creates a new additive batch.
- Keycloak was removed; `X-Demo-Role` is caller-controlled.

## Not yet specified or implemented

- Stress formulas, scenario parameters, capital impact and loss calculations.
- Risk thresholds, alert generation, alert explanations and notifications.
- Dataset approval semantics and the `ExposureBatchApproved` event, marked proposed in the HLD.
- Production identity, token validation, service-to-service authentication, rate limiting and per-user data isolation.
- Client-specific retention, regulatory definitions, recovery and deployment secrets.

The Phase 0 proposal, client Q&A and agreed workflow documents were not available for verification, so these rules must be agreed before client delivery.

## Verification

- Full Maven reactor tests pass in the current workspace: 21 tests passed.
- Frontend TypeScript compilation and Vite production build pass.
- Oracle migration execution, Kafka startup and end-to-end browser execution still require local verification.

