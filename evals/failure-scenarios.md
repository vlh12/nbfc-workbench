# Failure and recovery specifications

Not execution evidence. CURRENT cases preserve inspected behavior; TARGET/RECOMMENDED cases need referenced decisions. Use isolated synthetic data and no production credentials.

| ID | Class / trigger | Required observable result |
|---|---|---|
| FAIL-01 | CURRENT malformed UTF-8/header/quoted field/invalid decimal/empty/oversized report | Actionable bounded errors; no saved report; valid retry still works. |
| FAIL-02 | CURRENT file changed after validate, before Save report | Save revalidates new bytes; invalid change rejected; no partial report/loans. |
| FAIL-03 | TARGET cross-bank ID guessing, spoofed CSV/header or privileged client role | Server identity/tenant checks deny all unauthorized paths without disclosing existence/values. Demo is insufficient production security (DEC-05). |
| FAIL-04 | RECOMMENDED Oracle failure during activation/outbox/audit | No partial new active version; previous valid state intact; failure visible/retry safe (DEC-03/04/11). |
| FAIL-05 | RECOMMENDED outbox send succeeds but acknowledge/marking fails | Retry may duplicate event; projection dedup prevents double counting. Commit/delivery states separate. |
| FAIL-06 | RECOMMENDED duplicate, delayed or out-of-order snapshot event | No double count or rollback to older version; reviewed ordering rule preserves active scope (DEC-03/event schema). |
| FAIL-07 | RECOMMENDED projection worker unavailable | Accepted version retained; UI shows stale/pending projection/version, not empty or fabricated fresh data. |
| FAIL-08 | TARGET two concurrent uploads for one scope | Reviewed lock/optimistic version rule avoids lost update; loser gets actionable conflict; history coherent (DEC-03). |
| FAIL-09 | TARGET simultaneous queue claims/stale decisions | One reviewed owner/version wins; others conflict; no overwritten decision (DEC-06). |
| FAIL-10 | TARGET expired/released claim, nonlead override, missing reason | Apply agreed expiry/release policy; deny unauthorized override and empty reason; audit allowed lead action (DEC-06/11). |
| FAIL-11 | TARGET analyst self-review/identity with multiple roles | Server separation-of-duties policy enforced by actor identity, not UI role alone (DEC-05/06). |
| FAIL-12 | RECOMMENDED invalid/incomplete scenario or zero/missing denominator | Actionable failure; no default zero/NaN/success alert. No unapproved formula (DEC-07). |
| FAIL-13 | TARGET audit persistence fails | No silent success without the agreed durable audit guarantee; rollback/repair policy explicit (DEC-11). |
| FAIL-14 | RECOMMENDED correction/re-upload of active version | New candidate/version; old immutable history unchanged; rerun full validation/confirmation (DEC-03/10). |
| FAIL-15 | WORKSPACE missing Phase 0/Q&A or unreadable conversation reply | Evidence gap stays explicit; no fabricated requirements, no restricted external upload; independent work continues. |
| FAIL-16 | WORKSPACE missing tool, wrong JDK, offline dependencies, Oracle/Kafka stopped | Report check blocked with sanitized evidence; do not count it passed or silently upgrade stack. |

After a failure test verify state, history, outbox, projection and audit as applicable. Record cleanup of synthetic test records. Do not delete real environments, rewrite applied migrations or restart unrelated services to make tests pass.
