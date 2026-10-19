import { useEffect, useRef, useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../auth";
import { moneyLabel } from "../dashboardMetrics";
import { latestNpaPeriods, type QualityVersion } from "../riskExploration";
import { Button } from "./ui/button";
type Entry = { id: string; kind: string; status: string; createdAt: string };
type Point = ReturnType<typeof latestNpaPeriods>[number];
export function NpaHistoryChart({ bankId, nbfcId, entries }: { bankId: string; nbfcId: string; entries: Entry[] }) {
  const [points, setPoints] = useState<Point[] | null>(null), [loading, setLoading] = useState(false), [error, setError] = useState("");
  const generation = useRef(0);
  const versionKey = entries.filter(e => e.kind === "QUALITY" && e.status === "APPROVED").map(e => e.id).join("|");
  useEffect(() => { generation.current++; setPoints(null); setError(""); setLoading(false); return () => { generation.current++; }; }, [bankId, nbfcId, versionKey]);
  async function load() {
    const request = ++generation.current; setLoading(true); setPoints(null); setError("");
    try {
      const versions = entries.filter(e => e.kind === "QUALITY" && e.status === "APPROVED");
      const all: QualityVersion[] = [];
      // Read in bounded batches; a failed request blocks the chart rather than implying complete history.
      for (let i = 0; i < versions.length; i += 8) {
        const batch = await Promise.all(versions.slice(i, i + 8).map(async entry => ({ ...await api<Omit<QualityVersion, "id" | "createdAt">>("/api/portfolios/quality/" + entry.id), id: entry.id, createdAt: entry.createdAt })));
        if (request !== generation.current) return;
        all.push(...batch);
      }
      if (request === generation.current) setPoints(latestNpaPeriods(all, bankId, nbfcId));
    } catch { if (request === generation.current) setError("NPA history could not be loaded completely. Retry when all asset-quality versions are available."); }
    finally { if (request === generation.current) setLoading(false); }
  }
  return <section aria-label="NPA percentage history" className="mt-4 min-w-0 space-y-3 rounded-xl border border-slate-200 p-4">
    <div><h4 className="font-semibold">NPA percentage history</h4><p className="mt-1 text-sm text-slate-600">{nbfcId} · NPA_90_PLUS / total asset-quality buckets × 100</p><p className="mt-1 text-xs text-slate-500">Uses approved asset-quality versions in the latest 100 loaded input records. Latest approved version per reporting date; this may not cover the full history. Zero totals create gaps.</p></div>
    <Button disabled={loading} variant="outline" onClick={() => void load()}>{loading ? "Loading NPA history…" : "Load NPA history"}</Button>
    {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">{error}</p>}
    {points && <>
      {points.filter(p => p.percentage != null).length < 2 ? <p className="text-sm text-slate-500">At least two distinct reporting dates with positive totals are needed for a line chart. Save another asset-quality period for this NBFC.</p> : <div className="h-72 w-full min-w-0" aria-label="NPA percentage line chart; exact values in the history table"><ResponsiveContainer width="100%" height="100%"><LineChart data={points} margin={{ top: 12, right: 16, bottom: 8, left: 0 }}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="reportingDate" tick={{ fontSize: 11 }} /><YAxis domain={[0, 100]} tickFormatter={v => v + "%"} width={48} /><Tooltip content={({ active, payload }) => { const point = payload?.[0]?.payload as Point | undefined; return active && point ? <div className="rounded border border-slate-200 bg-white p-3 text-xs shadow"><p>{point.reportingDate} · {point.nbfcId}</p><p>NPA {point.percentage}% · {moneyLabel(point.npa)}</p><p>Total {moneyLabel(point.total)}</p><p>Version {point.id}</p></div> : null; }} /><Line name="NPA percentage" type="linear" dataKey="chartPercentage" stroke="#be185d" strokeWidth={2} dot={{ r: 4 }} connectNulls={false} isAnimationActive={false} /></LineChart></ResponsiveContainer></div>}
      <div className="overflow-x-auto"><table className="w-full text-left text-xs"><caption className="sr-only">NPA history exact values and source versions</caption><thead><tr><th className="py-2">Reporting date</th><th>NPA INR</th><th>Total INR</th><th>NPA %</th><th>Version</th></tr></thead><tbody>{points.map(point => <tr key={point.id} className="border-t"><td className="py-2 pr-3">{point.reportingDate}</td><td className="whitespace-nowrap pr-3">{moneyLabel(point.npa)}</td><td className="whitespace-nowrap pr-3">{moneyLabel(point.total)}</td><td className="pr-3">{point.percentage == null ? "Not available (zero total)" : point.percentage + "%"}</td><td className="font-mono" title={point.id}>{point.id.slice(0, 8)}</td></tr>)}</tbody></table></div>
      {!points.length && <p className="text-sm text-slate-500">No approved asset-quality history for this NBFC in the loaded records.</p>}
    </>}
  </section>;
}
