# Contributor increment

Task/outcome: <requested contributor change>. Apply master-implementation.md and context/DATA-CONTRACTS.md, USER-FLOWS.md and DECISIONS.md.

Trace both legacy batch and current saved-report APIs/UI. Preserve reference selection, complete-NBFC validation, explicit Save report, revalidation/history and exact figures. Explicit EAD is approved but candidate columns/limits and snapshot scope are not finalized. Settle the affected contract before edits. Never infer EAD from outstanding or replace report history with current references.

For an authorized target slice, implement the reviewed contract through parser/service/persistence/API/UI and synthetic tests. Verify invalid input saves nothing, cross-bank/object reads are denied, decimals are exact and old formats remain compatible. Snapshot activation/difference/correction/duplicate mechanics require their recorded decisions. Report actual checks, remaining decisions and rollback boundary.
