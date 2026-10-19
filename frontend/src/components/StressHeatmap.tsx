import { useState } from "react";
import { api } from "../auth";
import { moneyCents, moneyLabel } from "../dashboardMetrics";
import { stressPreview } from "../riskExploration";
import { Button } from "./ui/button";

type Entry = { id: string; kind: string; reportingDate: string | null; status: string };
type Portfolio = { id: string; bankId: string; reportingDate: string; status: string; rows: { nbfcId: string; eadAmount: string | null }[] };
type Preview = { cells: ReturnType<typeof stressPreview>[]; ead: string; capital: string; capitalLabel: string; medium: string; high: string; nbfc: string; capitalId: string; thresholdId: string };
const steps = [0, 20, 40, 60, 80, 100];
const input = "mt-1 w-full rounded border border-slate-300 p-2";
const colors: Record<string, string> = { LOW: "bg-teal-50 text-teal-950", MEDIUM: "bg-amber-100 text-amber-950", HIGH: "bg-rose-100 text-rose-950" };
export function StressHeatmap({ snapshot, entries }: { snapshot: Portfolio; entries: Entry[] }) {
  const nbfcs = [...new Set(snapshot.rows.map(row => row.nbfcId))];
  const [nbfc, setNbfc] = useState(nbfcs[0] || ""), [capitalId, setCapitalId] = useState(""), [thresholdId, setThresholdId] = useState("");
  const [preview, setPreview] = useState<Preview | null>(null), [active, setActive] = useState<ReturnType<typeof stressPreview> | null>(null);
  const [loading, setLoading] = useState(false), [error, setError] = useState("");
  function clear() { setPreview(null); setActive(null); setError(""); }
  async function generate() {
    clear(); setLoading(true);
    try {
      if (snapshot.status !== "APPROVED" || snapshot.rows.some(row => row.eadAmount == null)) throw new Error("Select an approved portfolio with complete explicit EAD and resolved reconciliation warnings.");
      const rows = snapshot.rows.filter(row => row.nbfcId === nbfc);
      if (!rows.length) throw new Error("Select an NBFC in this portfolio.");
      const eadCents = rows.reduce((sum, row) => sum + moneyCents(row.eadAmount!), 0n);
      const ead = (eadCents / 100n).toString() + "." + (eadCents % 100n).toString().padStart(2, "0");
      const [capital, thresholds] = await Promise.all([
        api<{ bankId: string; reportingDate: string; eligibleCapital: string; label: string }>("/api/portfolios/capital/" + capitalId),
        api<{ bankId: string; name: string; mediumPct: string; highPct: string }>("/api/portfolios/thresholds/" + thresholdId),
      ]);
      if (capital.bankId !== snapshot.bankId || thresholds.bankId !== snapshot.bankId) throw new Error("All preview inputs must belong to the same bank.");
      if (capital.reportingDate !== snapshot.reportingDate) throw new Error("Select capital matching the portfolio reporting date.");
      const cells = steps.flatMap(severity => steps.map(recovery => stressPreview(ead, capital.eligibleCapital, severity, recovery, thresholds.mediumPct, thresholds.highPct)));
      setPreview({ cells, ead, capital: capital.eligibleCapital, capitalLabel: capital.label, medium: thresholds.mediumPct, high: thresholds.highPct, nbfc, capitalId, thresholdId });
      setActive(cells[0]);
    } catch (e) { setError(e instanceof Error ? e.message : e && typeof e === "object" && "message" in e ? String(e.message) : "Preview could not be loaded. Retry when services are available."); }
    finally { setLoading(false); }
  }
  return <section aria-label="Severity–recovery heatmap" className="min-w-0 space-y-4 rounded-xl border border-slate-200 bg-white p-5">
    <div><h3 className="text-lg font-bold">Severity–recovery heatmap</h3><p className="mt-1 text-sm text-slate-600">Explore capital impact using the existing direct-loss model. Preview only: no scenario or alert is saved.</p><p className="mt-1 text-xs text-slate-500">Portfolio {snapshot.id.slice(0, 8)} · {snapshot.reportingDate} · INR</p></div>
    <div className="grid gap-3 sm:grid-cols-3">
      <label>Heatmap NBFC<select className={input} disabled={loading} value={nbfc} onChange={e => { setNbfc(e.target.value); clear(); }}><option value="">Select</option>{nbfcs.map(id => <option key={id}>{id}</option>)}</select></label>
      <label>Heatmap capital version<select className={input} disabled={loading} value={capitalId} onChange={e => { setCapitalId(e.target.value); clear(); }}><option value="">Select</option>{entries.filter(e => e.kind === "CAPITAL" && e.status === "APPROVED").map(e => <option key={e.id} value={e.id}>{e.reportingDate} · {e.id.slice(0, 8)}</option>)}</select></label>
      <label>Heatmap threshold version<select className={input} disabled={loading} value={thresholdId} onChange={e => { setThresholdId(e.target.value); clear(); }}><option value="">Select</option>{entries.filter(e => e.kind === "THRESHOLDS" && e.status === "APPROVED").map(e => <option key={e.id} value={e.id}>{e.id.slice(0, 8)} · APPROVED</option>)}</select></label>
    </div>
    <Button disabled={loading || !nbfc || !capitalId || !thresholdId} onClick={() => void generate()}>{loading ? "Loading heatmap…" : "Generate heatmap preview"}</Button>
    {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">{error}</p>}
    {preview && <>
      <p className="text-sm">{preview.nbfc} · EAD {moneyLabel(moneyCents(preview.ead))} · {preview.capitalLabel}: {moneyLabel(moneyCents(preview.capital))}</p>
      <p className="text-xs text-slate-500">Capital version {preview.capitalId} · Threshold version {preview.thresholdId}. LOW below {preview.medium}%; MEDIUM from {preview.medium}%; HIGH from {preview.high}%. Prototype settings.</p>
      <div className="overflow-x-auto"><table className="w-full min-w-[560px] border-separate border-spacing-1 text-center text-xs"><caption className="mb-2 text-left text-sm">Cell value: capital impact (%) · Severity ↓ / Recovery →</caption><thead><tr><th scope="col">Severity / Recovery</th>{steps.map(r => <th scope="col" key={r}>{r}% recovery</th>)}</tr></thead><tbody>{steps.map((s, row) => <tr key={s}><th scope="row">{s}% severity</th>{steps.map((r, col) => { const cell = preview.cells[row * steps.length + col]; return <td key={r}><button type="button" className={"w-full rounded p-3 outline-offset-2 focus-visible:outline-2 focus-visible:outline-slate-700 " + colors[cell.alert]} aria-label={"Severity " + s + "%, recovery " + r + "%, impact " + cell.impact + "%, " + cell.alert} title={"Loss " + moneyLabel(cell.lossCents) + "; impact " + cell.impact + "%"} onMouseEnter={() => setActive(cell)} onFocus={() => setActive(cell)} onClick={() => setActive(cell)}><strong>{cell.impact}%</strong><span className="mt-1 block">{cell.alert}</span></button></td>; })}</tr>)}</tbody></table></div>
      {active && <p role="status" className="rounded bg-slate-50 p-3 text-sm">Severity {active.severity}% · Recovery {active.recovery}% · Direct loss {moneyLabel(active.lossCents)} · Capital impact {active.impact}% · {active.alert}</p>}
      <p className="text-xs text-slate-500">Loss = explicit EAD × severity / 100 × (1 − recovery / 100). Impact = unrounded loss / selected capital × 100. To save an assessment, use the existing scenario form with its required asset-quality context and rationale.</p>
    </>}
  </section>;
}
