---
name: implement-governance
description: Extend NBFC assessment review, queue claiming, lead override or audit after the affected identity and transition decisions are resolved.
---

Read [supervisor prompt](../../prompts/supervisor.md), [flows](../../context/USER-FLOWS.md) and [failures](../../evals/failure-scenarios.md). Trace active governance controller/service/repositories; queue listing alone is not ownership. Demo roles do not establish trusted actors, banks or leads.

Resolve the affected claim/expiry/override policy first. Apply server-side bank/role checks and separation of duties. Recommend atomic claim/decision plus audit, conflict responses for stale/concurrent writers and reasoned lead overrides retaining prior decisions. Preserve existing assessment/follow-up behavior and compatibility routes.

Test distinct identities, two banks, concurrent claims, stale revisions, unauthorized/missing-reason overrides and state/audit rollback. Automatic dataset approval is separate from assessment approval. Report unresolved state mechanics and checks not executed.
