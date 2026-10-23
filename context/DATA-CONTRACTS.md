# Data contracts

## Status

USER-APPROVED: explicit ead_amount per exposure and INR rupees (U1/U2). All other candidate EAD-v1 columns/rules below are RECOMMENDED until reviewed. No current endpoint accepts this candidate. Never silently rename exposure_amount or derive EAD from outstanding_amount. EAD need not equal principal, outstanding or revenue; relationships require an agreed model.

## Observed legacy exposure CSV

Exact header: `bank_id,nbfc_id,exposure_amount`.

CsvValidator: UTF-8, optional BOM; skips blank lines; three unquoted fields; trims values; IDs match SYN-[A-Z0-9-]{1,40}; amounts nonnegative with 1-16 integer digits and up to two decimals; 1-10,000 records; bounded line errors and no batch saved on invalid input. ExposureEvent.Row uses exposureAmount, not EAD. Upload accepts multiple bank IDs, which does not meet target bank isolation. Same-file re-upload creates a distinct additive batch; delivery deduplication is by event ID.

## Observed saved lending-report CSV

Exact header:

```csv
bank_id,nbfc_id,loan_id,reporting_date,amount_lent,interest_amount,total_payable,amount_repaid,outstanding_amount,revenue_period_start,revenue_period_end,revenue_amount,currency,unit
```

Fourteen columns; UTF-8 with optional BOM; unquoted trimmed fields; 2 MiB max, 1-10,000 records, up to 100 reported issues. Synthetic bank/NBFC IDs up to 44 chars, loan IDs up to 64. Money nonnegative, up to 16 integer digits and 2 decimals. amount_lent > 0. Dates valid YYYY-MM-DD, years 0001-9999.

Rules: total_payable = amount_lent + interest_amount; amount_repaid <= total_payable; outstanding_amount = total_payable - amount_repaid; revenue_period_start <= revenue_period_end <= reporting_date; currency=INR; unit=RUPEE. Selected reference must exist; rows match bank/as-of date and contain one NBFC's complete unique loan set. Figures match reference values. All rows repeat one revenue period/value matching exactly one reference revenue record. Persist revenue once, not per loan. Save revalidates server-side. No EAD field exists here.

## Candidate EAD-v1: RECOMMENDED, not upload-ready

One exposure per row; one authenticated bank and as-of date per file. Scope bank/date vs bank/NBFC/date remains DEC-03. Multiple NBFCs are allowed only if approved scope permits. Fixture: data/synthetic/exposures-ead-v1.proposed.csv.

```csv
bank_id,nbfc_id,exposure_id,reporting_date,ead_amount,currency,unit
```

| Column | Candidate validation | Decision status |
|---|---|---|
| bank_id | Required SYN- uppercase letters/digits/hyphens, max 44 chars; equals trusted bank identity | Representation/limits recommended; bank scope approved |
| nbfc_id | Required same synthetic syntax/max 44 chars; belongs to allowed bank dataset | Recommended representation |
| exposure_id | Required synthetic ID, max 64 chars; unique within resolved snapshot scope | Recommended stable key; relation to loan_id unresolved |
| reporting_date | Required valid YYYY-MM-DD, 0001-9999; one date per file | Recommended field; future-date policy unresolved |
| ead_amount | Required explicit INR rupees; recommend nonnegative with 1-16 integer digits and at most 2 decimals | Field/units approved; zero/precision limits recommended |
| currency | Required literal INR | Currency approved; explicit column recommended |
| unit | Required literal RUPEE | Unit approved; explicit column recommended |

Transport recommendations: UTF-8 with optional BOM, exact header/order, unquoted fields, whitespace trimming and blank-line skipping consistent with existing parsers. Reject embedded commas, separators, symbols, exponent notation, NaN, infinity and blank required values. Recommend 2 MiB, 10,000 rows and 100 displayed errors to match saved-report guards. Agree and enforce limits at gateway/owning service. Reject missing/unknown/duplicate headers, duplicate exposure keys and mixed scope. Errors identify physical line, field, reason and safe correction hint; file-level errors use explicit file scope. Reject entire invalid file; no partial activation. Never reveal another bank's values in errors.

Keep BigDecimal/Oracle decimal precision end to end and monetary APIs as decimal strings. Never round input into validity. Agree calculation scale/rounding at a defined boundary. UI crore scaling cannot change stored rupees.

Do not add sector, rating, PD, LGD, recovery, severity, capital or collateral fields without reviewed use, unit, provenance and null policy. Missing scenario inputs cause actionable failure, not zero/default risk.

## Snapshot/event candidate

Recommend metadata outside CSV: schema version, server-derived scope, snapshot ID/version, previous version, status, uploader, timestamps, canonical hash and audit correlation. Contributor cannot choose active/approved status or uploader. Exact metadata/event schemas unresolved.

Recommend canonical duplicate detection within bank/snapshot scope using agreed ordering/decimal normalization. Raw-file hash and canonical hash have different purposes. Do not disclose cross-bank hashes. Duplicate events, duplicate files and legitimate later periods need separate tests.

Conceptual recommendation: draft -> invalid (correct/re-upload) or validated -> automatically approved after acceptance conditions/required contributor difference confirmation -> atomically activate a new version. These are not approved enum names. Preserve explicit Save report until reviewed integration. Automatic dataset approval and supervisor approval of an analyst assessment are separate decisions.
