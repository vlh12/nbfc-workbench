import { WorkbenchShell } from "./components/WorkbenchShell";
import type { WorkspaceView } from "./workspace";
import { PortfolioWorkbench } from "./components/PortfolioWorkbench";
import { LendingReferencePanel } from "./components/LendingReferencePanel";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { auth, api } from "./auth";
import { SupervisorPanel } from "./components/SupervisorPanel";
import { AssessmentSubmission } from "./components/AssessmentSubmission";
import { Button } from "./components/ui/button";

type Batch = { id: string; rowCount: number; status: string; createdAt: string };
type Problem = { message?: string; issues?: { line: number; message: string }[] };
function money(amount: string) {
  const [integer, fraction = ""] = amount.split(".");
  return "₹" + new Intl.NumberFormat("en-IN").format(BigInt(integer)) + "." + fraction.padEnd(2, "0");
}
export default function App() {
  const contributor = auth.hasRealmRole("CONTRIBUTOR");
  const analyst = auth.hasRealmRole("ANALYST") || auth.hasRealmRole("SUPERVISOR");
  const supervisor = auth.hasRealmRole("SUPERVISOR");
  const [view, setView] = useState<WorkspaceView>(contributor ? "data" : "dashboard");
  const [otherUploadsOpen, setOtherUploadsOpen] = useState(false);
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
      const batchRows = await api<Batch[]>("/api/batches");
      setBatches(batchRows); setLoaded(true); setServiceError("");
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
      setNotice(`Batch accepted: ${batch.rowCount} records validated. Legacy exposure processing continues through Kafka; portfolio analytics use Upload portfolio separately.`);
      await refresh();
    } catch (error) {
      setProblem(error && typeof error === "object" && "message" in error ? error as Problem : { message: "Upload did not complete. Check recent batches before retrying." });
    } finally { setBusy(false); }
  }
  const batchHistory = (<section className="rounded-xl border border-slate-200 bg-white p-6"><div className="flex items-center justify-between"><h3 className="text-lg font-bold">Recent validated batches</h3><Button variant="outline" onClick={() => void refresh()}>Refresh</Button></div><div className="overflow-x-auto"><table className="mt-4 w-full text-left text-sm"><caption className="sr-only">Latest fifty validated exposure batches</caption><thead><tr className="border-b"><th scope="col" className="py-3">Batch</th><th scope="col">Records</th><th scope="col">Validation</th><th scope="col">Submitted</th></tr></thead><tbody>{batches.map(b => <tr key={b.id} className="border-b border-slate-100"><td className="py-3 pr-4 font-mono text-xs">{b.id}</td><td>{b.rowCount}</td><td><span className="rounded bg-teal-50 px-2 py-1 text-teal-800">{b.status}</span></td><td>{new Date(b.createdAt).toLocaleString("en-IN")}</td></tr>)}</tbody></table></div>{!batches.length && <p className="mt-4 text-slate-500">{loaded ? "No validated batches yet." : "Loading…"}</p>}</section>);
  return <WorkbenchShell view={view} navigate={setView}>
      {!auth.authenticated ? <section className="rounded-xl border border-slate-200 bg-white p-8"><h2 className="text-2xl font-bold">Your exposure workbench</h2><p className="my-4 text-slate-600">Enter the local demo to upload synthetic exposures, then switch roles to view the analyst summary.</p><Button onClick={() => void auth.login()}>Enter demo</Button></section> : <>
        <PortfolioWorkbench view={view} navigate={setView} />
        {analyst && serviceError && <p role="alert" className="rounded-lg bg-amber-50 p-4 text-amber-900">{serviceError} <button className="underline" onClick={() => void refresh()}>Refresh</button></p>}
        {contributor && <details hidden={view!=="data" && view!=="all"} onToggle={e=>setOtherUploadsOpen(e.currentTarget.open)} className="rounded-xl border border-slate-200 bg-white">
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
        {analyst && <div hidden={view!=="all" && view!=="exposure"}>{batchHistory}</div>}
        {analyst && !supervisor && <div hidden={view!=="all" && view!=="assessments"}><AssessmentSubmission batches={batches} /></div>}
        {supervisor && <div hidden={view!=="all" && view!=="assessments"}><SupervisorPanel /></div>}
        {!contributor && !analyst && <p role="alert">Your account has no workbench role. Contact your administrator.</p>}
      </>}
  </WorkbenchShell>;
}
