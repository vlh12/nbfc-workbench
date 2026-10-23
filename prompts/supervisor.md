# Supervisor / lead increment

Task/outcome: <requested governance change>. Apply master-implementation.md and context/USER-FLOWS.md, DECISIONS.md and evals/failure-scenarios.md.

Follow current GovernanceController -> GovernanceService -> active repositories. Preserve SUBMITTED/RETURNED/APPROVED assessments, OPEN/COMPLETED follow-ups and audit. These are observed states, not approval for new transitions. Queue listing is not exclusive claim ownership; bank scope and lead override are incomplete.

Resolve claim/expiry/release semantics, trusted lead entitlement, allowed override and immutable prior-decision audit before dependent edits. Keep dataset automatic approval separate from analyst assessment approval. Implement authorized behavior with atomic claim/decision, trusted tenant checks, distinct actors and reason validation. Test concurrent claims, stale decisions, self-review, cross-bank access, ordinary-supervisor override denial, lead reason and audit rollback. Do not add a client-controlled production lead flag. Report actual checks and gaps.
