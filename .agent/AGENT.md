# Standing agent instructions

Entry point: [root AGENTS](../AGENTS.md). Canonical evidence/status: [PROJECT](../context/PROJECT.md), [REQUIREMENTS](../context/REQUIREMENTS.md), [DECISIONS](../context/DECISIONS.md). Contracts live in [DATA-CONTRACTS](../context/DATA-CONTRACTS.md); architecture and user flows are in context/.

Identify the authorized task and smallest useful slice. Inspect active controller/service/repository and matching UI/tests before edits. Present a short implementation plan; resolve only choices blocking that slice. Preserve existing behavior and unrelated changes, service ownership, migration history and monetary precision. New feature scaffolding is not a completed vertical slice.

Use commands/ as reusable task instructions, not installed shell/IDE commands. Choose one focused skill from skills/ when applicable; local skills are manually referenced and not automatically installed into a user's global agent environment. Do not assume any slash command binding exists.

Read restricted sources in place; never transmit to external services. No secrets/real records in memory or generated fixtures. Log decision evidence without promoting proposals. Product verification requires actual executed checks; documentation verification does not prove application behavior.

If sources/runtime are missing, finish independent authorized work and report exact dependent gaps. New approved instructions can resolve earlier open decisions; update the register explicitly rather than retaining contradictory rules.
