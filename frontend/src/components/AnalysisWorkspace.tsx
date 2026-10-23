import { entryLabel, scenarioName } from "../workflowLabels";
import { portfolioDetails } from "../portfolioLabels";
import { useEffect, useState } from "react";
import { moneyCents, moneyLabel, shareLabel } from "../dashboardMetrics";
import { sumExposure, scenarioRate, type AnalysisEntry, type AnalysisPortfolio, type AnalysisScenario } from "../analysisData";
import { KpiCard } from "./DashboardVisuals";
import { NpaHistoryChart } from "./NpaHistoryChart";
import { PeriodComparison } from "./PeriodComparison";
import { ScenarioComparison } from "./ScenarioComparison";
export function AnalysisWorkspace({ snapshot, entries, runs, runsLoaded, runsError, initialNbfc }: {
  initialNbfc?: string; snapshot: AnalysisPortfolio; entries: AnalysisEntry[]; runs: AnalysisScenario[]; runsLoaded: boolean; runsError: boolean;
}) {
  const [view, setView] = useState("nbfc"), [nbfc, setNbfc] = useState(snapshot.rows[0]?.nbfcId || ""), [showHistory, setShowHistory] = useState(false);
  const nbfcIds = [...new Set(snapshot.rows.map(row => row.nbfcId))].sort();
  useEffect(()=>{if(initialNbfc && snapshot.rows.some(row=>row.nbfcId===initialNbfc)){setNbfc(initialNbfc);setView("nbfc");setShowHistory(false);}},[initialNbfc,snapshot.id]);
  const loans = snapshot.rows.filter(row => row.nbfcId === nbfc), totals = sumExposure(loans), portfolio = sumExposure(snapshot.rows);
  const matchingRuns = runs.filter(run => run.bankId === snapshot.bankId && run.nbfcId === nbfc && run.snapshotId === snapshot.id);
  return <section aria-label="Analysis desk" className="min-w-0 space-y-5 rounded-xl border border-teal-200 bg-white p-5">
    <div><h3 className="text-xl font-bold">Analysis desk</h3><p className="mt-1 text-sm text-slate-600">Investigate one NBFC, compare reporting periods, and explain saved stress results.</p><p className="mt-1 break-words text-xs text-slate-500">Selected portfolio {portfolioDetails(snapshot)} · {snapshot.reportingDate} · {snapshot.status} · {snapshot.bankId}</p></div>
    <nav aria-label="Analysis tools" className="flex flex-wrap gap-2 border-b border-slate-200 pb-3">{[["nbfc", "NBFC analysis"], ["period", "Period comparison"], ["scenario", "Scenario comparison"]].map(([id, label]) => <button type="button" key={id} aria-pressed={view === id} onClick={() => setView(id)} className={"rounded-lg px-4 py-2 text-sm font-semibold " + (view === id ? "bg-teal-800 text-white" : "bg-slate-100 text-slate-700")}>{label}</button>)}</nav>
    {view !== "period" && <label className="block max-w-md text-sm">Analysis NBFC<select className="mt-1 w-full rounded border border-slate-300 p-2" value={nbfc} onChange={e => { setNbfc(e.target.value); setShowHistory(false); }}><option value="">Select an NBFC</option>{nbfcIds.map(id => <option key={id}>{id}</option>)}</select></label>}
    <div hidden={view !== "nbfc"}>
      <section aria-label="NBFC analysis details" className="space-y-4">
        {!nbfc ? <p>Select an NBFC to inspect its saved evidence.</p> : <>
          <div className="grid gap-3 sm:grid-cols-3"><KpiCard label="NBFC loan count" value={String(totals.loans)} detail={nbfc} /><KpiCard label="NBFC explicit EAD" value={totals.ead == null ? "Incomplete EAD" : moneyLabel(totals.ead)} detail="Selected portfolio only" /><KpiCard label="NBFC portfolio share" value={portfolio.ead == null || totals.ead == null ? "Incomplete EAD" : portfolio.ead === 0n ? "No positive EAD" : shareLabel(totals.ead, portfolio.ead)} detail="Denominator: complete portfolio explicit EAD" /></div>
          {snapshot.status !== "APPROVED" && <p className="rounded bg-amber-50 p-3 text-sm">This portfolio is reviewable but not approved for stress. Resolve reconciliation warnings and missing EAD before simulation.</p>}
          <div className="overflow-x-auto"><table className="w-full text-left text-xs"><caption className="mb-2 text-left font-semibold">Loans for {nbfc}</caption><thead><tr><th className="py-2">Loan</th><th>Amount lent INR</th><th>Outstanding INR</th><th>EAD INR</th><th>DPD / status</th></tr></thead><tbody>{loans.map(loan => <tr key={loan.loanId} className="border-t"><td className="py-2 pr-3">{loan.loanId}</td><td className="whitespace-nowrap pr-3">{moneyLabel(moneyCents(loan.amountLent))}</td><td className="whitespace-nowrap pr-3">{moneyLabel(moneyCents(loan.outstandingAmount))}</td><td className="whitespace-nowrap pr-3">{loan.eadAmount == null ? "Not supplied" : moneyLabel(moneyCents(loan.eadAmount))}</td><td>{loan.dpdDays} / {loan.loanStatus}</td></tr>)}</tbody></table></div>
          <p className="text-xs text-slate-500">Loan DPD and NBFC borrower asset-quality inputs describe different datasets.</p>
          <button type="button" aria-expanded={showHistory} onClick={() => setShowHistory(!showHistory)} className="rounded border border-slate-300 px-4 py-2 text-sm font-semibold">{showHistory ? "Hide NBFC asset-quality history" : "View NBFC asset-quality history"}</button>
          {showHistory && <NpaHistoryChart bankId={snapshot.bankId} nbfcId={nbfc} entries={entries} />}
          <div><h4 className="font-semibold">Saved stress results for this NBFC and portfolio</h4><p className="mt-1 text-xs text-slate-500">Filtered from the latest 100 loaded runs. Includes failed runs with their recorded reason. Use Scenario comparison to compare other portfolio versions.</p>
            {runsError ? <p role="alert" className="mt-3 text-sm text-red-900">Saved stress results could not be loaded.</p> : !runsLoaded ? <p className="mt-3 text-sm">Loading saved stress results…</p> : !matchingRuns.length ? <p className="mt-3 text-sm text-slate-500">No saved runs for this NBFC and portfolio in the loaded records.</p> : <div className="mt-3 space-y-3">{matchingRuns.map(run => <article key={run.id} className="rounded border border-slate-200 p-3 text-sm"><p className="break-words font-medium">{scenarioName(run)} · {run.status}</p><p className="mt-1 text-xs text-slate-500">{new Date(run.createdAt).toLocaleString("en-IN")} · Model {run.modelVersion || "Not recorded"}</p>{run.result ? <p className="mt-2">Severity {scenarioRate(run, "severityPct") ?? "Not recorded"}% · Recovery {scenarioRate(run, "recoveryPct") ?? "Not recorded"}% · Loss {moneyLabel(moneyCents(run.result.stressedLoss))} · Impact {run.result.capitalImpactPct}% · {run.result.alertSeverity}</p> : <p className="mt-2 text-amber-900">{run.failureReason || "No completed result recorded."}</p>}</article>)}</div>}
          </div>
        </>}
      </section>
    </div>
    <div hidden={view !== "period"}><PeriodComparison entries={entries} selected={snapshot.id} /></div>
    <div hidden={view !== "scenario"}><ScenarioComparison key={nbfc} runs={runs} entries={entries} bankId={snapshot.bankId} nbfcId={nbfc} loaded={runsLoaded} loadError={runsError} /></div>
  </section>;
}
