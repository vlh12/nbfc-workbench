# PDF report visuals

The existing Portfolio evidence report PDF now includes an overview, Top 10 NBFC EAD bar chart, Top 5 plus Others exposure doughnut, latest six completed scenario impact bars, and an eligible saved-input severity-recovery heatmap. Existing loan details, scenario evidence and warnings remain in the report.

The EAD charts aggregate only the selected immutable portfolio. Shares, Others membership, exact amounts and rounding use the website helpers. Monetary inputs stay as decimal strings and BigInt; approximate numbers are used only for chart geometry. Missing EAD blocks exposure charts instead of becoming zero. Zero totals show an explicit empty state. Completed scenario bars are independent alternatives, never summed; displayed scale and saved alert colours are explicit.

The heatmap uses the existing stressPreview helper and the latest eligible phase0-direct-loss-v1 saved run in the selected bank/portfolio. Its inputs and threshold IDs are printed and the grid is labelled as an unsaved preview. It does not capture unsaved website selections or publish new scenarios. Asset-quality history and prior-period charts are not included because their separately loaded versions are not part of this export contract.

Implementation: frontend/src/pdfReport.ts, vendor/pdf-lib.d.ts, and report-card description in components/RiskDashboard.tsx. No backend calculation, threshold or approval logic changed. The PDF library remains the locally bundled pdf-lib 1.17.1 with its MIT notice.

Validation: six browser download fixtures cover incomplete EAD, empty portfolio, Unicode, multiple NBFCs/Others, zero EAD, and the saved data behind the supplied September report. PDF signatures, expected contents, bank/portfolio scoping and page boundaries passed; rendered pages were inspected. Frontend typecheck/build and all 126 Maven tests passed on 2026-10-22.

## Single-scenario interpretation

The latest completed scenario also has a Scenario at a glance page: saved loss/impact/alert cards, a threshold-position gauge, and a severity-versus-impact scatter plot. The scatter holds the saved whole-percent recovery constant and reuses stressPreview with 0-100% severity in 10-point steps. Grey dots are derived previews, never additional stored runs; the highlighted blue point displays the recorded scenario severity and recorded impact. Previewing is limited to the existing phase0-direct-loss-v1 model with positive EAD/capital. For unsupported or fractional-recovery cases the saved result remains and an explicit preview-unavailable message is shown. Axis geometry is approximate, underlying values stay exact.

Two advisory workflow sentences ask the user to confirm inputs, submit the assessment for Supervisor review, monitor exposure and rerun when inputs change. They do not approve assessments or infer regulatory compliance. The supplied September report's saved 25% severity / 5% recovery point is highlighted at 5.7% capital impact. The PDF download was regenerated and rendered; exact stored amounts and the saved-point label were checked.

## Plain-language report order

The main PDF now starts with the latest completed calculation: what happened, why the saved alert has its band, and practical Analyst/Supervisor/follow-up workflow steps. It explicitly distinguishes a completed calculation from an actual loss and from Supervisor approval. The first-page data check is a short sentence for warning-free snapshots; actual warnings are listed in readable row/message form.

Visuals follow the main interpretation. The gauge/scatter page explains its meaning in plain words, followed by portfolio charts, a clearly labelled heatmap preview, exact loan details, and short reference IDs at the end. The full JSON block is no longer printed in the PDF. Download evidence JSON remains unchanged and available separately; original saved inputs/results are not edited. References retain run, portfolio, model, input versions, original rationale and exact monetary outputs.

Advice follows the documented workflow in docs/phase0-implementation.md: check same-date inputs and rationale, submit a saved completed result, Supervisor approve/return with a reason, and owner/due-date/evidence verification for follow-up. The original Q&A is not available in the synced sources and was not represented as reviewed. No new financial limits, calculations or approval rules were introduced.

Validation covers result-first ordering, readable warnings, absence of raw JSON in PDF, preserved exact values, reference IDs, bank/portfolio scope, empty and failed cases, and all existing chart fixtures. Final PDFs were rendered and inspected. Frontend type checking/build and the Maven regression suite passed.

## Analyst submission and Supervisor review

Supervisor PDF downloads now fetch the primary saved scenario's exact review history through the read-only, Supervisor-only `GET /api/governance/scenarios/{scenarioId}/review-history` endpoint. This lookup is not limited to the latest 100 assessments. It does not submit, approve, return, or update anything.

The result and plain-language next steps remain first. The next section shows three website-style status cards: calculation, analyst submission, and Supervisor decision. It includes recorded submission notes and dates, decision reasons and dates, related follow-up actions, and the chronological audit sequence. Account references stay in the short appendix. Approval is separate from the prototype risk alert and from follow-up closure.

Only records for the exact scenario and selected portfolio are displayed. The newest submission controls the summary. A missing response or restricted role is labelled as unavailable, never treated as proof of approval or non-submission. Each loaded history shows when it was fetched; download again after a new decision. Analyst downloads retain the calculation and charts but do not fetch Supervisor-only records.

Validation: frontend typecheck and build; complete Maven test suite including five new endpoint authorization/history tests; eleven browser PDF download fixtures covering approved, returned, pending, unavailable, restricted, empty and long reports. PDF text and page bounds checked, with all actual report pages rendered for visual review.


## Approval required for PDF export

User-requested change: PDF generation is locked until the latest completed scenario for the selected portfolio has a recorded Supervisor approval, decision actor and date. The newest submission controls eligibility; an older approval cannot unlock a newer pending/returned assessment. Approval is polled every five seconds and fetched again on download. Missing/unavailable/mismatched history blocks generation. Both the exporter and PDF builder enforce the check. The PDF includes only that approved scenario, so unreviewed scenarios cannot enter its charts or appendix. Portfolio charts remain descriptive portfolio data. JSON/CSV exports are unchanged. No approval transition or calculation was changed.
