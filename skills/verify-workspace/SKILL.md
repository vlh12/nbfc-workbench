---
name: verify-workspace
description: Validate this NBFC agent workspace locally for file completeness, relative links, skill frontmatter and synthetic EAD fixture arithmetic without testing the application.
---

Run [scripts/validate_workspace.py](scripts/validate_workspace.py) from the repository root using a discovered Python 3 interpreter. It uses only the standard library, performs no network calls and does not modify application files. It validates the proposed fixture as documentation, not as proof of upload acceptance or approved business rules.

For product verification use [testing prompt](../../prompts/testing.md) and [OPS](../../context/OPS.md). Do not substitute this documentation check for Maven, frontend, Oracle, Kafka or browser verification. Compare application hashes/diff when preservation is part of the task. Inspect any failures, fix only the relevant workspace file and rerun the affected checks.
