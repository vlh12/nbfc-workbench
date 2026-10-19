import { PortfolioWorkbench } from "./components/PortfolioWorkbench";
import { LendingReferencePanel } from "./components/LendingReferencePanel";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { auth, api } from "./auth";
import { SupervisorPanel } from "./components/SupervisorPanel";
import { AssessmentSubmission } from "./components/AssessmentSubmission";
import { Button } from "./components/ui/button";
import LendingReportReview from "./components/LendingReportReview";

type Total = { nbfcId: string; amount: string; records: number };
type Batch = { id: string; rowCount: number; status: string; createdAt: string };
type Problem = { message?: string; issues?: { line: number; message: string }[] };
function money(amount: string) {
  const [integer, fraction = ""] = amount.split(".");
  return "₹" + new Intl.NumberFormat("en-IN").format(BigInt(integer)) + "." + fraction.padEnd(2, "0");
}
function totalAmount(rows: Total[]) {
  const cents = rows.reduce((sum, row) => {
    const [integer, fraction = ""] = row.amount.split(".");
    return sum + BigInt(integer) * 100n + BigInt(fraction.padEnd(2, "0"));
  }, 0n);
  return (cents / 100n).toString() + "." + (cents % 100n).toString().padStart(2, "0");
}
export default function App() {
  const contributor = auth.hasRealmRole("CONTRIBUTOR");
  const analyst = auth.hasRealmRole("ANALYST") || auth.hasRealmRole("SUPERVISOR");
  const supervisor = auth.hasRealmRole("SUPERVISOR");
  const [otherUploadsOpen, setOtherUploadsOpen] = useState(false);
  const [rows, setRows] = useState<Total[]>([]);
  const [batches, setBatches] = useState<Batch[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [synthetic, setSynthetic] = useState(false);
  const [busy, setBusy] = useState(false);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [serviceError, setServiceError] = useState("");
  const [notice, setNotice] = useState("");
  const [loaded, setLoaded] = useState(false);
  const refresh = useCallback(async () => {
    if (!auth.authenticated || !(analyst || (contributor && otherUploadsOpen))) return;
    try {
      const [batchRows, totals] = await Promise.all([
        api<Batch[]>("/api/batches"),
        analyst ? api<Total[]>("/api/exposures/summary") : Promise.resolve([]),
      ]);
      setBatches(batchRows); setRows(totals); setLoaded(true); setServiceError("");
    } catch { setServiceError("Data could not be refreshed. Please try again."); }
  }, [contributor, analyst, otherUploadsOpen]);
  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => void refresh(), 5000);
    return () => window.clearInterval(timer);
  }, [refresh]);
  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file || !synthetic) return;
    setBusy(true); setProblem(null); setNotice("");
    const form = new FormData();
    form.append("file", file); form.append("synthetic", "true");
    try {
      const batch = await api<Batch>("/api/batches", { method: "POST", body: form });
      setNotice(`Batch accepted: ${batch.rowCount} records validated. The exposure summary will update after processing.`);
      await refresh();
    } catch (error) {
      setProblem(error && typeof error === "object" && "message" in error ? error as Problem : { message: "Upload did not complete. Check recent batches before retrying." });
    } finally { setBusy(false); }
  }
  const batchHistory = (<section className="rounded-xl border border-slate-200 bg-white p-6"><div className="flex items-center justify-between"><h3 className="text-lg font-bold">Recent validated batches</h3><Button variant="outline" onClick={() => void refresh()}>Refresh</Button></div><div className="overflow-x-auto"><table className="mt-4 w-full text-left text-sm"><caption className="sr-only">Latest fifty validated exposure batches</caption><thead><tr className="border-b"><th scope="col" className="py-3">Batch</th><th scope="col">Records</th><th scope="col">Validation</th><th scope="col">Submitted</th></tr></thead><tbody>{batches.map(b => <tr key={b.id} className="border-b border-slate-100"><td className="py-3 pr-4 font-mono text-xs">{b.id}</td><td>{b.rowCount}</td><td><span className="rounded bg-teal-50 px-2 py-1 text-teal-800">{b.status}</span></td><td>{new Date(b.createdAt).toLocaleString("en-IN")}</td></tr>)}</tbody></table></div>{!batches.length && <p className="mt-4 text-slate-500">{loaded ? "No validated batches yet." : "Loading…"}</p>}</section>);
  return <div className="min-h-screen">
    <header className="border-b border-slate-200 bg-white px-6 py-5">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4">
        <div><p className="text-xs font-bold uppercase tracking-widest text-teal-800">Synthetic data prototype</p><h1 className="mt-1 text-xl font-bold">NBFC Exposure Stress & Alert Workbench</h1></div>
        <div className="flex items-center gap-3">
          {auth.authenticated && <label className="text-sm">Demo role <select aria-label="Demo role" value={auth.role ?? "CONTRIBUTOR"} onChange={e => auth.setRole(e.target.value as "CONTRIBUTOR" | "ANALYST" | "SUPERVISOR")} className="ml-2 rounded border border-slate-300 p-2"><option value="CONTRIBUTOR">Contributor</option><option value="ANALYST">Analyst</option><option value="SUPERVISOR">Supervisor</option></select></label>}
          <Button variant="outline" onClick={() => auth.authenticated ? void auth.logout() : void auth.login()}>{auth.authenticated ? "Exit demo" : "Enter demo"}</Button>
        </div>
      </div>
    </header>
    <main className="mx-auto max-w-6xl space-y-6 p-6">
      <p role="note" className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">Local demo: roles are selectable and no real authentication is provided. Use synthetic data only.</p>
      {!auth.authenticated ? <section className="rounded-xl border border-slate-200 bg-white p-8"><h2 className="text-2xl font-bold">Your exposure workbench</h2><p className="my-4 text-slate-600">Enter the local demo to upload synthetic exposures, then switch roles to view the analyst summary.</p><Button onClick={() => void auth.login()}>Enter demo</Button></section> : <>
        {!contributor && <div><h2 className="text-2xl font-bold">Exposure overview</h2><p className="mt-1 text-slate-600">Validated synthetic exposure records · INR</p></div>}
        <PortfolioWorkbench />
        {analyst && <LendingReportReview formatMoney={money} />}
        {analyst && serviceError && <p role="alert" className="rounded-lg bg-amber-50 p-4 text-amber-900">{serviceError} <button className="underline" onClick={() => void refresh()}>Refresh</button></p>}
        {analyst && <>
          <div className="grid gap-4 sm:grid-cols-3">{[["Total exposure", money(totalAmount(rows))], ["NBFCs represented", String(rows.length)], ["Exposure records", String(rows.reduce((s, r) => s + r.records, 0))]].map(([label, value]) => <article key={label} className="rounded-xl border border-slate-200 bg-white p-5"><p className="text-sm text-slate-600">{label}</p><p className="mt-3 break-words text-2xl font-bold">{loaded ? value : "Loading…"}</p></article>)}</div>
          <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">Exposure by NBFC</h3><p className="mb-5 text-sm text-slate-500">Simple totals. No stress assumptions or risk thresholds have been applied.</p>
            {!rows.length ? <p className="py-10 text-center text-slate-500">{loaded ? "No processed exposures yet. Upload the synthetic sample to begin." : "Loading exposure summary…"}</p> : <>
              <div className="h-72" aria-label="Exposure by NBFC chart; exact amounts are in the table below"><ResponsiveContainer width="100%" height="100%"><BarChart data={rows.map(r => ({ ...r, chartAmount: Number(r.amount) }))}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="nbfcId" /><YAxis tickFormatter={v => "₹" + (Number(v) / 100000).toFixed(1) + "L"} /><Tooltip /><Bar dataKey="chartAmount" name="Exposure (INR)" fill="#0f766e" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer></div>
              <div className="overflow-x-auto"><table className="mt-4 w-full text-sm"><caption className="sr-only">Exact exposure totals in INR by NBFC</caption><thead><tr className="border-b text-left"><th scope="col" className="py-3">NBFC</th><th scope="col" className="text-right">Exposure (INR)</th><th scope="col" className="text-right">Records</th></tr></thead><tbody>{rows.map(r => <tr key={r.nbfcId} className="border-b border-slate-100"><td className="py-3">{r.nbfcId}</td><td className="text-right font-medium">{money(r.amount)}</td><td className="text-right">{r.records}</td></tr>)}</tbody></table></div>
            </>}
          </section>
        </>}
        {contributor && <details onToggle={e=>setOtherUploadsOpen(e.currentTarget.open)} className="rounded-xl border border-slate-200 bg-white">
          <summary className="cursor-pointer px-5 py-4 text-sm font-semibold text-slate-600">Other upload formats <span className="ml-2 font-normal text-slate-400">Reference reports and simple exposure files</span></summary>
          <div className="space-y-5 border-t border-slate-100 p-5">
            <p className="text-sm text-slate-500">Use these for existing reference-based reports or the three-column exposure format. For your multi-NBFC CSV, use Upload portfolio above.</p>
            {serviceError && <p role="alert" className="rounded bg-amber-50 p-3 text-sm">{serviceError}</p>}
        {otherUploadsOpen && <LendingReferencePanel formatMoney={money} />}
        <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">Upload synthetic exposures</h3><p className="mt-2 text-sm text-slate-600">UTF-8 CSV · Maximum 2 MB and 10,000 records · Columns: bank_id, nbfc_id, exposure_amount. Use IDs beginning SYN-.</p>
          <a href="/workbench-demo-16.csv" download className="my-3 inline-block text-sm font-semibold text-teal-800 underline">Download 16-record synthetic sample</a>
          <form onSubmit={upload} className="space-y-4"><label className="block text-sm font-medium">Exposure CSV<input type="file" accept=".csv,text/csv" required disabled={busy} onChange={e => { setFile(e.target.files?.[0] ?? null); setProblem(null); setNotice(""); }} className="mt-2 block w-full rounded-lg border border-slate-300 p-3" /></label><label className="flex items-center gap-3 text-sm"><input type="checkbox" checked={synthetic} onChange={e => setSynthetic(e.target.checked)} required disabled={busy} />I confirm this file contains synthetic data only.</label><Button disabled={!file || !synthetic || busy}>{busy ? "Validating…" : "Validate and upload"}</Button></form>
          {notice && <p role="status" className="mt-4 rounded-lg bg-teal-50 p-4 text-teal-900">{notice}</p>}
          {problem && <div role="alert" className="mt-4 rounded-lg bg-red-50 p-4 text-red-900"><p className="font-semibold">{problem.message ?? "Upload rejected."}</p>{problem.issues && <ul className="mt-2 list-disc pl-5">{problem.issues.map((issue, i) => <li key={i}>Line {issue.line}: {issue.message}</li>)}</ul>}</div>}
        </section>
            {batchHistory}
          </div>
        </details>}
        {analyst && batchHistory}
        {analyst && !supervisor && <AssessmentSubmission batches={batches} />}
        {supervisor && <SupervisorPanel />}
        {!contributor && !analyst && <p role="alert">Your account has no workbench role. Contact your administrator.</p>}
      </>}
    </main>
  </div>;
}
