# User journeys and internal workflow

## Contributor: current

Select synthetic bank reference -> choose one NBFC's complete report CSV -> confirm synthetic -> Validate report -> correct/re-upload -> explicitly Save report -> receive Report ID -> inspect history. Save revalidates and atomically stores report/loans. Charts use the selected Report ID's saved figures. No EAD or active-snapshot replacement.

## Contributor: target and recommended mechanics

USER-APPROVED goals: explicit EAD, INR rupees, versioned replacement, automatic dataset approval, bank scope. Recommend: authenticate -> choose allowed scope -> upload -> validate all rows -> correct in-app or re-upload -> compare against current version -> contributor confirms material replacement if required -> accept -> automatic dataset approval -> atomic activation -> asynchronous version-specific analytics. Prior version immutable. Scope/materiality/confirmation/activation details unresolved. This introduces no supervisor dataset approval gate.

## Analyst

Current: open saved reports for demo bank -> choose Report ID -> read saved loan figures/chart/revenue. Separate legacy exposure summary and assessment submission exist.

Target recommendation: choose active or explicitly historical snapshot -> inspect bank/NBFC exposure -> select approved scenario version -> run reproducible stress -> see inputs/calculation/threshold explanation -> document interpretation -> submit assessment referencing dataset/run/model versions. Never infer EAD, invent regulatory thresholds or present uncomputed scenarios as successful runs.

## Supervisor and lead

Current: list assessments -> approve/return -> follow-up -> complete/reopen -> audit. Queue endpoint is a listing, not a claim lock.

USER-APPROVED target: shared queue and lead override with audit. Recommend: authorized bank-scoped supervisor sees eligible items -> atomically claims -> reviews immutable submitted inputs -> approve/return with reason -> follow-up. Concurrent loser gets conflict without overwriting ownership. Lead override requires trusted entitlement, nonblank reason, superseded-decision reference and audit. Claim expiry/release/escalation and whether override reassigns or reverses decisions remain unresolved. Do not treat a selectable demo lead header as production authorization.

## Internal lifecycle/failures

Separate validation, automatic dataset approval, analytics delivery, analyst submission and supervisor decision. Recommend: committed accepted data survives Kafka downtime, with pending delivery visible and retried by outbox. Projection failure cannot invent a new version or add all history to active totals. Expose projection version/freshness.

Audit recommendation: actor/effective role, bank/scope, entity/version, action, before/after references, reason, timestamp and correlation; redact sensitive payloads. Persist state and audit atomically. Retention/tamper-evidence unresolved.
