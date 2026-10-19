import { useState } from "react";
import { moneyCents, moneyLabel } from "../dashboardMetrics";
import { riskDrivers, scenarioRate, versionWarnings, type AnalysisEntry, type AnalysisScenario } from "../analysisData";
import { Button } from "./ui/button";
const labels = ["Baseline", "Moderate", "Severe"];
const input = "mt-1 w-full rounded border border-slate-300 p-2";
export function ScenarioComparison({ runs, entries, bankId, nbfcId, loaded, loadError }: {
  runs: AnalysisScenario[]; entries: AnalysisEntry[]; bankId: string; nbfcId: string; loaded: boolean; loadError: boolean;
}) {
  const [ids, setIds] = useState(["", "", ""]), [compared, setCompared] = useState<{ label: string; run: AnalysisScenario }[] | null>(null), [error, setError] = useState("");
  const candidates = runs.filter(run => run.bankId === bankId && run.nbfcId === nbfcId && run.status === "COMPLETED" && run.result);
  function compare() {
    setCompared(null); setError("");
    const chosen = ids.map((id, index) => ({ label: labels[index], run: candidates.find(run => run.id === id) })).filter((item): item is { label: string; run: AnalysisScenario } => !!item.run);
    if (!ids[0] || chosen.length < 2) { setError("Select a baseline and at least one other completed scenario for this NBFC."); return; }
    if (new Set(chosen.map(item => item.run.id)).size !== chosen.length) { setError("Choose different saved runs for each comparison column."); return; }
    setCompared(chosen);
  }
  const shown = (value?: string) => value || "Not recorded";
  const money = (value?: string) => value == null ? "Not recorded" : moneyLabel(moneyCents(value));
  const fields: [string, (run: AnalysisScenario) => string][] = [
    ["Run ID", r => r.id], ["Created", r => new Date(r.createdAt).toLocaleString("en-IN")],
    ["Reporting date", r => entries.find(e => e.id === r.snapshotId)?.reportingDate || "Not in loaded inputs"],
    ["Portfolio version", r => r.snapshotId], ["Model", r => shown(r.modelVersion)],
    ["Severity", r => scenarioRate(r, "severityPct") == null ? "Not recorded" : scenarioRate(r, "severityPct") + "%"],
    ["Recovery", r => scenarioRate(r, "recoveryPct") == null ? "Not recorded" : scenarioRate(r, "recoveryPct") + "%"],
    ["Explicit EAD", r => money(r.result?.eadAmount)], ["Eligible capital", r => money(r.result?.eligibleCapital)],
    ["Capital definition", r => shown(r.result?.capitalLabel)], ["Direct loss", r => money(r.result?.stressedLoss)],
    ["Capital impact", r => r.result!.capitalImpactPct + "%"], ["Alert", r => r.result!.alertSeverity],
    ["Capital version", r => shown(r.inputs?.capitalId)], ["Asset-quality version", r => shown(r.inputs?.qualityId)],
    ["Threshold version", r => shown(r.result?.thresholdVersion)],
    ["Threshold settings", r => "MEDIUM ≥ " + r.result!.mediumPct + "% · HIGH ≥ " + r.result!.highPct + "%"],
    ["Rationale", r => shown(r.inputs?.rationale)],
  ];
  return <section aria-label="Saved scenario comparison" className="space-y-4">
    <div><h4 className="font-semibold">Saved scenario comparison</h4><p className="mt-1 text-sm text-slate-600">{nbfcId} · Compare completed saved runs. Baseline, Moderate and Severe are comparison labels you assign here; saved records stay unchanged.</p><p className="mt-1 text-xs text-slate-500">Candidates come from the latest 100 loaded runs for the same bank and NBFC. Different portfolio, capital, context, threshold and model versions are flagged.</p></div>
    {loadError && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">Saved scenarios are unavailable. Retry when Stress Simulation is available.</p>}
    {!loaded && !loadError && <p className="text-sm text-slate-500">Loading saved scenarios…</p>}
    <div className="grid gap-3 sm:grid-cols-3">{labels.map((label, index) => <label key={label}>{label} saved scenario<select className={input} disabled={!loaded || loadError} value={ids[index]} onChange={e => { setIds(old => old.map((value, i) => i === index ? e.target.value : value)); setCompared(null); setError(""); }}><option value="">{index ? "Optional" : "Select baseline"}</option>{candidates.map(run => <option key={run.id} value={run.id} disabled={ids.some((id, i) => i !== index && id === run.id)}>{run.id.slice(0, 8)} · Severity {scenarioRate(run, "severityPct") ?? "?"}% / Recovery {scenarioRate(run, "recoveryPct") ?? "?"}% · {run.result!.alertSeverity}</option>)}</select></label>)}</div>
    <Button disabled={!loaded || loadError} onClick={compare}>Compare saved scenarios</Button>
    {loaded && !loadError && candidates.length < 2 && <p className="text-sm text-slate-500">Save at least two completed scenarios for this NBFC to compare different assumptions.</p>}
    {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">{error}</p>}
    {compared && !loadError && <div role="region" aria-label="Scenario comparison results" className="space-y-4">
      <div className="overflow-x-auto"><table className="w-full min-w-[600px] table-fixed text-left text-xs"><caption className="sr-only">Recorded assumptions, results and versions</caption><thead><tr><th className="w-36 py-2">Recorded field</th>{compared.map(item => <th key={item.label}>{item.label}</th>)}</tr></thead><tbody>{fields.map(([label, field]) => <tr key={label} className="border-t"><th scope="row" className="py-2 pr-3 font-medium">{label}</th>{compared.map(item => <td key={item.label} className="break-words p-2 align-top">{field(item.run)}</td>)}</tr>)}</tbody></table></div>
      {compared.slice(1).map(item => { const warnings = versionWarnings(compared[0].run, item.run); return <div key={item.label} className="space-y-3 rounded-xl border border-slate-200 p-4"><h5 className="font-semibold">{item.label} versus Baseline</h5>{warnings.length > 0 && <div role="note" className="rounded bg-amber-50 p-3 text-sm text-amber-950"><p className="font-medium">Check comparability</p><ul className="mt-2 list-disc space-y-1 pl-5">{warnings.map(message => <li key={message}>{message}</li>)}</ul></div>}<div aria-label={item.label + " risk-driver explanation"}><h6 className="font-semibold">Risk-driver explanation</h6><ul className="mt-2 list-disc space-y-2 pl-5 text-sm">{riskDrivers(compared[0].run, item.run).map(note => <li key={note}>{note}</li>)}</ul></div></div>; })}
    </div>}
  </section>;
}
