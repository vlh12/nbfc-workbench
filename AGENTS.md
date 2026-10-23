# Project rules

Use synthetic data only. Read docs/requirements-status.md before changing business behavior. Keep access and data modules logically separate. Preserve four business service boundaries. Do not add stress formulas, regulatory thresholds or approval transitions without reviewed requirements and user approval. Report unexecuted checks honestly. Run Maven tests, frontend type checking and a production build before claiming verification. Do not edit parent sources/ files.

## AI agent workspace (2026-10-16)

Start with [context/PROJECT.md](context/PROJECT.md) for evidence and [context/DECISIONS.md](context/DECISIONS.md) for decision status. Read [context/REQUIREMENTS.md](context/REQUIREMENTS.md), [context/DATA-CONTRACTS.md](context/DATA-CONTRACTS.md) and [context/USER-FLOWS.md](context/USER-FLOWS.md) before business changes. These distinguish current saved-report/legacy behavior from requested targets; older docs/ status claims are historical where code/current README differs.

Use [prompts/master-implementation.md](prompts/master-implementation.md) for incremental implementation. Preserve the existing rules above. Newly approved high-level goals do not settle schema columns, stress formulas, thresholds or transition mechanics. Explicit ead_amount and INR rupees are approved; candidate EAD-v1 remains proposed and current validators do not accept it. Finalize the contract before implementation.

Synthetic only. Preserve four services, existing code/versions/migrations and the working saved-report flow. Automatic dataset approval is distinct from supervisor assessment decisions and does not silently remove explicit Save report. Bank scope must ultimately use trusted identity; X-Demo-Role is local demo only. Queue claims and lead override are targets, not complete features.

Parent sources/ is read-only and currently empty. Phase 0/client Q&A analysis remains pending. Never send restricted originals/excerpts/embeddings or credentials to external services. Do not invent unavailable conversation replies or source requirements. No push/deploy/publication is authorized by workspace creation.

For documentation-only changes validate workspace links/fixtures/skill syntax and preservation of application files; do not claim application tests passed. Product changes retain the Maven/typecheck/build verification rule above. Discover commands/versions locally; see [context/OPS.md](context/OPS.md). Repository memory is local project guidance, separate from global assistant memory.
