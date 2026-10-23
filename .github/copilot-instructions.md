# Repository guidance for Copilot

Follow [root AGENTS.md](../AGENTS.md), then [PROJECT](../context/PROJECT.md), [DECISIONS](../context/DECISIONS.md) and the task-specific contract/flow. Use [master prompt](../prompts/master-implementation.md) for implementation increments.

Preserve existing application code unless a new implementation task authorizes changes. Keep four business services and existing versions/migrations/working flows. Synthetic data only; exact INR rupees, explicit EAD target, no implicit derivation. Proposed EAD-v1 is not accepted by current upload APIs. Distinguish observed, approved, recommended and unresolved.

No restricted source text, credentials or real financial data in prompts sent to external systems, logs or artifacts. Read missing Phase 0/Q&A locally when available; do not invent their contents. Demo roles are not production identity. Snapshot approval differs from supervisor assessment approval. Inspect actual commands before running and report checks honestly. Do not push/publish/deploy or add AI dependencies under this workspace task.
