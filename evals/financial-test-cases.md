# Financial cases and independent expected values

Specifications only; execution status must be recorded separately. Decimal strings below are INR rupees. No stress model/regulatory threshold is approved here.

| ID | Status | Inputs/action | Exact expectation |
|---|---|---|---|
| FIN-01 | Candidate EAD-v1 / DEC-02 | SYN-NBFC-001 exposures 1200000.00 + 800000.00; SYN-NBFC-002 500000.00, all SYN-BANK-001 | NBFC totals 2000000.00 and 500000.00; bank total 2500000.00. data/processed/exposures-ead-v1.expected.json. |
| FIN-02 | CURRENT saved report | Lent 1000000000.00; interest 60000000.00; repaid 200000000.00 | Payable 1060000000.00; outstanding 860000000.00. Not an EAD formula. |
| FIN-03 | CURRENT saved report | Three rows in data/raw/lending-report-demo.csv | Lent 1400000000.00; interest 81000000.00; payable 1481000000.00; repaid 250000000.00; outstanding 1231000000.00. Revenue is 180000000.00 once, not 540000000.00. |
| FIN-04 | Candidate boundary / DEC-02 | 0.10 + 0.20 | Exactly 0.30, using decimal arithmetic; binary float artifacts rejected in displayed/API money. |
| FIN-05 | CURRENT syntax / candidate recommendation | -1.00, 1.001, 1e6, NaN, blank, currency symbol, grouped commas | Reject rather than round/coerce. EAD zero acceptance remains to be reviewed; current validators allow zero monetary fields except amount_lent. |
| FIN-06 | Candidate precision / DEC-02 | 9999999999999999.99 and 10000000000000000.00 | First fits proposed 16+2 field limit, second rejected. Aggregate precision/overflow must be specified separately; no truncation. |
| FIN-07 | TARGET snapshot / DEC-03 | Current scope EAD=2500000.00 replaced by 2100000.00 | Active total 2100000.00, not 4600000.00. Prior version total remains 2500000.00; other bank unchanged. |
| FIN-08 | CURRENT presentation | 10000000.00 INR rendered on crore axis | Coordinate 1 crore; tooltip/API retains exact 10000000.00 rupees. |
| FIN-09 | CURRENT data meaning | Same revenue period repeated on three loan rows; another submission repeats it | One revenue per selected report. Do not sum duplicates or compare overlapping unequal periods as growth. |
| FIN-10 | UNRESOLVED model gate / DEC-07 | Missing EAD/severity/recovery/denominator for a proposed calculation | No successful computed risk result; actionable missing-input error. Exact formula/test values must be approved before a numerical risk expectation is added. |
| FIN-11 | Future reviewed model / DEC-07 | Result exactly on threshold; below/above by minimum agreed increment | Classification follows approved inclusive/exclusive rule and rounding order. Threshold value is not supplied here. |
| FIN-12 | TARGET explicit EAD | Explicit EAD differs from outstanding/principal | Preserve supplied EAD. Relationship validation only if separately approved; never silently overwrite. |

Use hand-calculated Decimal/BigDecimal expected values independent of implementation. Specify units and rounding at every reviewed calculation boundary. The local workspace checker validates FIN-01/03 fixture arithmetic and FIN-04 decimal arithmetic only; it does not run product financial tests.
