---
name: implement-ingestion
description: Implement an authorized NBFC contributor CSV or versioned-ingestion increment while preserving legacy batch and saved-report contracts.
---

Read [data contracts](../../context/DATA-CONTRACTS.md), [decisions](../../context/DECISIONS.md) and [contributor prompt](../../prompts/contributor.md). Inspect actual parser/controller/service/UI/tests first. Candidate EAD-v1 is not accepted by existing endpoints and needs review before product edits.

After contract approval, implement explicit EAD as BigDecimal INR rupees through a narrow compatible vertical slice. Keep saved-report explicit save and reference validation working. Add new migrations rather than rewriting applied history. For snapshots resolve scope, activation, canonical duplicates and difference-confirmation semantics first. Distinguish event delivery deduplication from upload deduplication.

Test whole-file rejection, decimal/date/key boundaries, missing/extra loans where applicable, tenant authorization, immutable version history and rollback. Use [financial cases](../../evals/financial-test-cases.md) and synthetic fixtures only. Report schema/version compatibility and executed vs unavailable checks.
