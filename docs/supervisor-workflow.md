# Supervisor workflow

Risk Governance is the microservice responsible for assessment decisions, follow-up actions and audit history. It runs on port 8084 and owns the governance tables. The frontend supervisor panel provides this workflow.

## Analyst submission

An Analyst selects a validated batch and submits an assessment note:

~~~text
POST /api/governance/assessments
{
  "batchId": "<validated-batch-id>",
  "note": "Assessment context"
}
~~~

The service records the submitter, timestamp and `SUBMITTED` status.

## Supervisor review

The Supervisor can list the queue and decide each assessment:

- `GET /api/governance/assessments`
- `GET /api/governance/assessments/queue`
- `POST /api/governance/assessments/{id}/decision` with `APPROVED` or `RETURNED`
- `POST /api/governance/assessments/{id}/approve` as an approval alias

Each decision stores the note, actor and timestamp. Approved assessments cannot be decided again. Returned assessments can be reviewed again.

## Follow-up management

Supervisors can create actions with a title, owner and due date:

- `POST /api/governance/assessments/{id}/follow-ups`
- `GET /api/governance/follow-ups`
- `PATCH /api/governance/follow-ups/{id}` with `OPEN` or `COMPLETED`

The UI supports completing and reopening actions. Every change is recorded in the audit table.

## Audit history

`GET /api/governance/audit` returns the latest governance events. Events are written for assessment submission, approval, return, follow-up creation and follow-up status changes.

## Local verification

1. Start Oracle and Kafka.
2. Start Access & Data, Exposure Analytics and Gateway with the original upload compound.
3. Start Risk Governance separately on port 8084.
4. Start the frontend and upload the synthetic sample as Contributor.
5. Switch to Analyst, submit the validated batch, then switch to Supervisor.
6. Review, approve or return the assessment, add a follow-up and inspect the audit history.

The original upload gateway intentionally does not expose `/api/governance/**`. For a single URL deployment, add and verify that route to Risk Governance and update the frontend proxy only after approval.

Dataset approval is marked proposed in the HLD and is not wired to ingestion status because client approval rules and downstream event semantics were not supplied. Stress formulas, risk thresholds and alert rules are also pending the Phase 0 proposal and client Q&A.

