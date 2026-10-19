# Acceptance cases

These are reusable specifications, not executed tests. CURRENT = observed regression to preserve. TARGET = approved goal with mechanics possibly unresolved. RECOMMENDED = proposed behavior requiring agreement. Missing source acceptance remains DEC-12. A target case becomes executable after its referenced choices are resolved; a table entry is not approval.

| ID | Class / requirement | Given / action | Expected result and evidence |
|---|---|---|---|
| AC-01 | CURRENT REQ-01 | Contributor sends valid synthetic fourteen-column report for a selected reference, validates then saves | Validation succeeds, explicit save revalidates, one report plus loans saved, Report ID visible; verify existing API/UI. |
| AC-02 | CURRENT REQ-09 | Report has duplicate/missing loan, wrong date/revenue/amount | Row/file errors; no saved report; correct complete file and retry. |
| AC-03 | CURRENT REQ-06 | Contributor calls reviewer endpoint; Analyst/Supervisor calls validation/save | Role-denied; missing authentication rejected. |
| AC-04 | CURRENT REQ-09 | Reviewer selects one Report ID, changes chart series | Exact saved values; revenue once; no current-reference substitution or cumulative-history totals. Pagination boundaries enforced. |
| AC-05 | CURRENT REQ-09 | Same event delivered twice to analytics | No duplicate projection for that event. A separate uploaded batch remains additive today. |
| AC-06 | TARGET REQ-03/10 | Finalized EAD contract uploaded with explicit 1200000.00 and 800000.00 | Exact 2000000.00 INR EAD; no implicit derivation; existing contracts stay compatible. DEC-02 required. |
| AC-07 | TARGET REQ-04 | Approved replacement changes two exposures and removes one in the same resolved scope | Current view equals replacement only; prior immutable version readable; other scopes unchanged. DEC-03 required. |
| AC-08 | TARGET REQ-05 | Valid candidate meets agreed acceptance/confirmation conditions | Dataset auto-approved without supervisor dataset gate; approval/delivery/assessment statuses separate; current Save behavior preserved until reviewed integration. DEC-04 required. |
| AC-09 | RECOMMENDED REC-01/02 | Duplicate candidate or material replacement | Duplicate policy applied per canonical scope; material replacement waits for contributor confirmation; old active version unchanged until acceptance. DEC-03/10 required. |
| AC-10 | TARGET REQ-06 | Two banks and distinct real identities use list/detail/upload/queue/override paths | Cross-bank data/actions denied, trusted identity determines scope, errors do not disclose other-bank values. DEC-05 required. |
| AC-11 | TARGET REQ-07 | Two supervisors try to claim one eligible item concurrently | Exactly one owner; loser conflict; authorized review only; immutable referenced input. DEC-06 required. |
| AC-12 | TARGET REQ-07/08 | Ordinary supervisor attempts lead override; lead retries without/with reason | Unauthorized/blank-reason denied; reviewed allowed override preserves previous decision and audit. DEC-06/11 required. |
| AC-13 | RECOMMENDED REC-05 | Approved model/scenario runs twice on identical versioned inputs | Same exact financial results/explanations and threshold classification; explicit versions/rounding. DEC-07/14 required. |
| AC-14 | TARGET REQ-08 | Failure during material state change/audit persistence | Transaction rolls back or follows approved durable audit policy; no unaudited successful mutation. DEC-11 required. |
| AC-15 | RECOMMENDED REC-04 | Oracle commits acceptance but Kafka is unavailable | Accepted state not lost; outbox pending/retry; duplicate delivery harmless; freshness visible. DEC-03/04/event design required. |
| AC-16 | CURRENT REQ-09 | Demo mode disabled | Protected APIs remain inaccessible; no header bypass. |
| AC-17 | WORKSPACE REQ-09 | Agent workspace created | Required files/links valid, fixtures synthetic, skill frontmatter valid, existing application hashes unchanged. Application runtime is not verified by this check. |

Per execution record: case ID, approved decision version, command/action, synthetic fixture, expected/actual, pass/fail/blocked, environment and date. Redact secrets. Unit/mock evidence is separate from live tenant, Oracle/Kafka and browser evidence.
