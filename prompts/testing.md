# Testing / verification increment

Task/outcome: <change or check requested>. Read AGENTS.md, context/OPS.md, DATA-CONTRACTS.md and evals/. Distinguish existing regression cases from target/recommended cases pending decisions. Inspect current tests/build scripts and tool versions; no assumed npm test, Oracle or Kafka availability.

Choose focused behavior checks and independently calculated expected outputs. For product changes run relevant backend tests plus frontend typecheck/build, then relevant isolated Oracle/Kafka/browser checks. For documentation-only work run the workspace validator and verify preserved application hashes. Do not mutate source originals or send restricted content externally.

Report exact checks executed, pass/fail/skipped reason, synthetic fixtures used, isolation/runtime limits and any untested acceptance condition. Historical test totals and mocked/unit results do not establish live integration. Stop once the change's required checks pass unless failures or new edits justify more runs.
