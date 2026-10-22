VERSION 3 - COMPLETE SYNTHETIC WORKFLOW PACK

The database is intentionally empty after cleanup. These files are not automatically imported.
All figures are synthetic. Money is INR rupees. CSV schema and direct-loss formula are unchanged.

1. Contributor: upload portfolios/portfolio-2026-09-30.csv. Name it September 2026 Monthly Exposure Portfolio. Confirm synthetic data, Validate, then Save. Status is Pending review.
2. Contributor: Supporting data -> Bank capital. Use the September row of supporting-data/bank-capital.csv. Add a matching asset-quality entry for SYN-NBFC-001 from nbfc-asset-quality.csv. All seven buckets are required. They describe the NBFC's borrower portfolio, not the bank loan CSV.
3. Supervisor: Review & Follow-up -> Review contributor data. Select each named portfolio/capital/quality input. Inspect the figures, add a reason, and approve each. Return incorrect versions with instructions. The contributor uploads a new corrected version; old versions remain evidence.
4. Supervisor: Thresholds. Save Capital Impact Policy - Medium 5% - High 10%. These are demonstration settings, not regulatory limits. Optional comparison policy: Medium 4%, High 8%.
5. Analyst: select the approved September portfolio. Select SYN-NBFC-001 and matching approved September capital and asset quality. Select the named threshold policy. Enter a scenario name, severity, recovery and a meaningful rationale from scenario-plan.csv. Calculate and save.
6. Analyst: open the completed scenario, write the assessment note and submit to Supervisor. Assessment status is available under Assessments.
7. Supervisor: Review analyst scenarios. Review assumptions/result and approve or return with a reason. Optionally add follow-up work, submit evidence, verify and close it using the existing action controls.
8. Supervisor: Downloads & Reports. The PDF unlocks only when the latest completed scenario's newest assessment is approved. It includes the approved scenario, readable review history and charts. JSON/CSV are separate data exports.

To exercise trends, first upload and approve the July and August portfolios and their matching support. There are 16 loans and 8 NBFCs per period, covering multiple loan statuses and the Others chart group. Add quality for all 8 NBFCs if running scenarios for each one.

Scenario checks for September NBFC 001, EAD 1200 crore and capital 5000 crore:
Baseline: severity20 recovery30 -> loss168 crore, impact3.36%, LOW.
Moderate: severity40 recovery30 -> loss336 crore, impact6.72%, MEDIUM.
Severe: severity60 recovery30 -> loss504 crore, impact10.08%, HIGH.
Use three scenario names for comparison. Do not add alternative scenario losses together.

Validation examples: duplicate loan must be rejected; missing EAD and outstanding mismatch remain review-only and cannot be approved. Correct the data before resubmission.

Supporting-data CSVs are entry guides. The website uses its existing supporting-data forms; they are not portfolio-upload files. demo-data.json holds the same inputs for authenticated test tooling. The older full-demo importer uses retired role headers and must not be used for this login-based workflow.
