---
name: implement-analytics
description: Build an authorized NBFC exposure, scenario or analyst-view increment with exact money, reproducibility and reviewed financial definitions.
---

Read [analyst prompt](../../prompts/analyst.md), [architecture](../../context/ARCHITECTURE.md) and [financial cases](../../evals/financial-test-cases.md). Preserve saved-report figures and do not aggregate historical submissions as new lending. Current Stress Simulation is scaffold.

Confirm the exact financial model, EAD inputs, units, rounding, scenario version and threshold boundaries before implementing stress/alerts. Missing definitions remain unresolved; never invent regulatory values or silently use outstanding as EAD. An arithmetic illustration is not an approved risk model.

Bind results to immutable input/run/model versions. For snapshot projections verify replacement, idempotency, ordering and freshness after scope/event decisions are approved. Use hand-calculated decimal expectations and explicit missing-input/error states. Report actual product checks per OPS.md.
