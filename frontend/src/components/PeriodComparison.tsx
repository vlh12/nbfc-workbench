import { portfolioLabel, portfolioDetails } from "../portfolioLabels";
import { useState } from "react";
import { api } from "../auth";
import { moneyLabel } from "../dashboardMetrics";
import { periodComparison, requestMessage, signedMoney, type AnalysisEntry, type AnalysisPortfolio } from "../analysisData";
import { Button } from "./ui/button";
const input = "mt-1 w-full rounded border border-slate-300 p-2";
export function PeriodComparison({ entries, selected }: { entries: AnalysisEntry[]; selected: string }) {
  const [priorId, setPriorId] = useState(""), [currentId, setCurrentId] = useState(selected);
  const [result, setResult] = useState<{ prior: AnalysisPortfolio; current: AnalysisPortfolio; rows: ReturnType<typeof periodComparison> } | null>(null);
  const [error, setError] = useState(""), [loading, setLoading] = useState(false);
  function clear() { setResult(null); setError(""); }
  const versions = entries.filter(e => e.kind === "PORTFOLIO" && e.status !== "VALIDATION_FAILED");
  async function compare() {
    clear(); setLoading(true);
    try {
      const [prior, current] = await Promise.all([api<AnalysisPortfolio>("/api/portfolios/" + priorId), api<AnalysisPortfolio>("/api/portfolios/" + currentId)]);
      if (prior.status === "VALIDATION_FAILED" || current.status === "VALIDATION_FAILED") throw new Error("Select successfully saved portfolios.");
      setResult({ prior, current, rows: periodComparison(prior, current) });
    } catch (e) { setError(requestMessage(e)); } finally { setLoading(false); }
  }
  return <section aria-label="Portfolio period comparison" className="space-y-4">
    <div><h4 className="font-semibold">Portfolio period comparison</h4><p className="mt-1 text-sm text-slate-600">Compare explicit EAD and reported outstanding across two different reporting dates for the same bank.</p><p className="mt-1 text-xs text-slate-500">Choose exact saved versions from the latest 100 loaded input records. No automatic active version is assumed.</p></div>
    <div className="grid gap-3 sm:grid-cols-2">{[["Prior portfolio", priorId, setPriorId], ["Current portfolio", currentId, setCurrentId]].map(([label, value, setter]) => <label key={String(label)}>{String(label)}<select className={input} disabled={loading} value={value as string} onChange={e => { (setter as (v: string) => void)(e.target.value); clear(); }}><option value="">Select</option>{versions.map(e => <option key={e.id} value={e.id}>{portfolioLabel(e)}</option>)}</select></label>)}</div>
    <Button disabled={loading || !priorId || !currentId} onClick={() => void compare()}>{loading ? "Comparing periods…" : "Compare portfolio periods"}</Button>
    {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">{error}</p>}
    {result && <div role="region" aria-label="Period comparison results" className="space-y-3">
      <p className="break-words text-sm">{result.current.bankId} · Prior {result.prior.reportingDate} ({portfolioDetails(result.prior)}) → Current {result.current.reportingDate} ({portfolioDetails(result.current)}) · INR</p>
      {(result.prior.warnings.length > 0 || result.current.warnings.length > 0 || result.prior.status !== "APPROVED" || result.current.status !== "APPROVED") && <p className="rounded bg-amber-50 p-3 text-sm text-amber-950">One or both selected versions are not approved or have warnings. These are reported figures, not stress-ready inputs.</p>}
      <p className="text-xs text-slate-500">Absent means not listed in that selected snapshot; it does not prove that a facility closed. Absent entries contribute zero only to this snapshot comparison. Missing EAD stays unavailable.</p>
      <div className="overflow-x-auto"><table className="w-full text-left text-xs"><caption className="sr-only">Exposure changes by NBFC</caption><thead><tr><th className="py-2 pr-3">NBFC / change</th><th>Prior EAD</th><th>Current EAD</th><th>EAD change</th><th>EAD change %</th><th>Outstanding change</th></tr></thead><tbody>{result.rows.map(row => <tr key={row.nbfcId} className="border-t"><td className="py-3 pr-3">{row.nbfcId}<span className="mt-1 block text-slate-500">{row.change}</span></td><td className="whitespace-nowrap pr-3">{!row.before.loans ? "Not listed" : row.before.ead == null ? "Incomplete EAD" : moneyLabel(row.before.ead)}</td><td className="whitespace-nowrap pr-3">{!row.after.loans ? "Not listed" : row.after.ead == null ? "Incomplete EAD" : moneyLabel(row.after.ead)}</td><td className="whitespace-nowrap pr-3">{row.delta == null ? "Not available" : signedMoney(row.delta)}</td><td className="pr-3">{row.changePct}</td><td className="whitespace-nowrap">{signedMoney(row.outstandingDelta)}</td></tr>)}</tbody></table></div>
    </div>}
  </section>;
}
