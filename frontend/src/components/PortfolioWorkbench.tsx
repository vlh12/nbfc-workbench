import { DashboardOverview, ScenarioBrowser, ReportsPanel, UserGuide, AssessmentList } from "./RiskDashboard";
import { Icon } from "./WorkbenchShell";
import type { WorkspaceView } from "../workspace";
import { exportPortfolioReport } from "../reportExport";
import { PortfolioExposureChart } from "./PortfolioExposureChart";
import { portfolioLabel, portfolioDetails } from "../portfolioLabels";
import { AnalysisWorkspace } from "./AnalysisWorkspace";
import type { AnalysisScenario as Run } from "../analysisData";
import { StressHeatmap } from "./StressHeatmap";
import { NpaHistoryChart } from "./NpaHistoryChart";
import { PortfolioIndicators } from "./PortfolioIndicators";
import { DoughnutChart } from "./DashboardVisuals";
import { moneyCents } from "../dashboardMetrics";
import { ContributorUpload } from "./ContributorUpload";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, auth } from "../auth";
import { Button } from "./ui/button";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";

type Issue = { line: number; field: string; message: string };
type Loan = { loanId: string; nbfcId: string; amountLent: string; outstandingAmount: string; eadAmount: string | null; dpdDays: number; loanStatus: string; instrumentType: string; collateralType: string; revenueAmount: string; revenuePeriodStart: string; revenuePeriodEnd: string };
type Entry = { name?: string | null; id: string; kind: string; reportingDate: string | null; status: string; createdAt: string; rowCount: number };
type Snapshot = { name?: string | null; id: string; bankId: string; reportingDate: string; status: string; rows: Loan[]; warnings: Issue[] };
type Validation = { rows: Loan[]; warnings: Issue[]; errors: Issue[] };
type Nbfc = { nbfcId: string; loans: number; amountLent: string; outstandingAmount: string; eadAmount: string | null; portfolioSharePct: string | null; loanStatusAmounts: Record<string, string> };
type Analysis = { snapshotId: string; totalLent: string; totalOutstanding: string; totalEad: string | null; concentrationDenominator: string; nbfcs: Nbfc[] };
type Quality = { bankId: string; nbfcId: string; reportingDate: string; buckets: Record<string, string>; totalAmount: string };
const buckets = ["CURRENT", "DPD_1_30", "DPD_31_60", "DPD_61_90", "NPA_90_PLUS", "RESTRUCTURED", "WRITE_OFF_RECOVERY"];
const input = "mt-1 w-full rounded border border-slate-300 p-2";
const box = "rounded-xl border border-slate-200 bg-white p-5";
function rupees(s: string | null | undefined) {
  if (s == null) return "Not supplied";
  const [whole, decimals = ""] = s.split(".");
  return "₹" + BigInt(whole).toLocaleString("en-IN") + "." + decimals.padEnd(2, "0");
}
function errorMessage(e: unknown) { return e && typeof e === "object" && "message" in e ? String(e.message) : "Request failed. Check the service and retry."; }
function cents(s: string) { const [i, d = ""] = s.split("."); return BigInt(i) * 100n + BigInt(d.padEnd(2, "0")); }
function delta(a: string, b: string) { const n = cents(a) - cents(b); const v = n < 0n ? -n : n; return (n < 0n ? "−" : "+") + rupees((v / 100n).toString() + "." + (v % 100n).toString().padStart(2, "0")); }

export function PortfolioWorkbench({ view = "all", navigate = () => {} }: { view?: WorkspaceView; navigate?: (view: WorkspaceView) => void }) {
  const show = (...views: WorkspaceView[]) => view === "all" || views.includes(view);
  const [refreshTick, setRefreshTick] = useState(0), [lastUpdated, setLastUpdated] = useState("");
  const contributor = auth.hasRealmRole("CONTRIBUTOR"), supervisor = auth.hasRealmRole("SUPERVISOR");
  const analyst = auth.hasRealmRole("ANALYST"), reviewer = analyst || supervisor;
  const [contributorView, setContributorView] = useState<"upload" | "saved" | "support">("upload");
  const [supportView, setSupportView] = useState<"capital" | "quality">("capital");
  const [reportingPeriod, setReportingPeriod] = useState(""), [analysisNbfc, setAnalysisNbfc] = useState("");
  const [entries, setEntries] = useState<Entry[]>([]), [selected, setSelected] = useState("");
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null), [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [error, setError] = useState(""), [notice, setNotice] = useState(""), [busy, setBusy] = useState(false);
  const [portfolioName, setPortfolioName] = useState("");
  const [file, setFile] = useState<File | null>(null), [synthetic, setSynthetic] = useState(false);
  const [validation, setValidation] = useState<Validation | null>(null);
  const [capital, setCapital] = useState(""), [capitalLabel, setCapitalLabel] = useState(""), [date, setDate] = useState("");
  const [qualityNbfc, setQualityNbfc] = useState(""), [qualityAmounts, setQualityAmounts] = useState<Record<string, string>>({});
  const [qualityTotal, setQualityTotal] = useState("");
  const [thresholdName, setThresholdName] = useState(""), [medium, setMedium] = useState(""), [high, setHigh] = useState("");
  const [thresholdError, setThresholdError] = useState("");
  const [savedThreshold, setSavedThreshold] = useState<{ id: string; name: string; mediumPct: string; highPct: string } | null>(null);

  const [nbfc, setNbfc] = useState(""), [capitalId, setCapitalId] = useState(""), [qualityId, setQualityId] = useState(""), [thresholdId, setThresholdId] = useState("");
  const [severity, setSeverity] = useState(""), [recovery, setRecovery] = useState(""), [rationale, setRationale] = useState("");
  const [runs, setRuns] = useState<Run[]>([]), [currentRun, setCurrentRun] = useState<Run | null>(null), [assessmentNote, setAssessmentNote] = useState("");
  const [qualityCurrent, setQualityCurrent] = useState(""), [qualityPrior, setQualityPrior] = useState(""), [trend, setTrend] = useState<{ current: Quality; prior: Quality | null } | null>(null);

  const [runsLoaded, setRunsLoaded] = useState(false), [runsError, setRunsError] = useState(false);
  const [trendError, setTrendError] = useState(""), [trendLoading, setTrendLoading] = useState(false);

  const refresh = useCallback(async () => {
    try { const rows = await api<Entry[]>("/api/portfolios"); setEntries(rows); setSelected(old => old || rows.find(e => e.kind === "PORTFOLIO" && e.status !== "VALIDATION_FAILED")?.id || ""); }
    catch (e) { setError(errorMessage(e)); }
  }, []);
  useEffect(() => { void refresh(); const timer = window.setInterval(() => void refresh(), 5000); return () => clearInterval(timer); }, [refresh]);
  useEffect(() => {
    if (!selected) return;
    let live = true; setSnapshot(null); setAnalysis(null);
    void api<Snapshot>("/api/portfolios/" + selected).then(s => { if (live) { setSnapshot(s); setDate(s.reportingDate); setNbfc(s.rows[0]?.nbfcId || ""); } }).catch(e => { if (live) setError(errorMessage(e)); });
    if (reviewer) void api<Analysis>("/api/exposures/portfolios/" + selected).then(a => { if (live) { setAnalysis(a); setLastUpdated(new Date().toLocaleTimeString("en-IN")); } }).catch(e => { if (live) setError(errorMessage(e)); });
    return () => { live = false; };
  }, [selected, reviewer, refreshTick]);
  useEffect(() => {
    if (!reviewer) return;
    const load = () => { void api<Run[]>("/api/scenarios").then(rows => { setRuns(rows); setRunsLoaded(true); setRunsError(false); }).catch(() => { setRunsError(true); }); };
    load(); const timer = window.setInterval(load, 5000); return () => clearInterval(timer);
  }, [reviewer, refreshTick]);
  async function perform(action: () => Promise<unknown>, message: string) {
    setBusy(true); setError(""); setNotice("");
    try { await action(); setNotice(message); await refresh(); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  }
  async function validateFile(event: FormEvent) {
    event.preventDefault(); if (!file || !synthetic || !portfolioName.trim()) return;
    await perform(async () => { const data = new FormData(); data.set("file", file); data.set("synthetic", "true"); setValidation(await api<Validation>("/api/portfolios/validate", { method: "POST", body: data })); }, "Validation complete. Review warnings before saving.");
  }
  async function saveFile() {
    if (!file || !synthetic || !portfolioName.trim() || !validation || validation.errors.length) return;
    await perform(async () => { const data = new FormData(); data.set("file", file); data.set("synthetic", "true"); data.set("name", portfolioName.trim()); const s = await api<Snapshot>("/api/portfolios", { method: "POST", body: data }); setSelected(s.id); setReportingPeriod(s.reportingDate); setValidation(null); setFile(null); setPortfolioName(""); setContributorView("saved"); }, "Portfolio saved. Analyst and supervisor can now select this snapshot.");
  }
  const post = (path: string, data: unknown) => api(path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) });
  async function saveThreshold(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const values = { bankId: "SYN-BANK-001", name: thresholdName.trim(), mediumPct: medium.trim(), highPct: high.trim() };
    setBusy(true); setThresholdError(""); setSavedThreshold(null); setNotice("");
    try {
      const saved = await api<Entry>("/api/portfolios/thresholds", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(values) });
      setSavedThreshold({ id: saved.id, name: values.name, mediumPct: values.mediumPct, highPct: values.highPct });
      await refresh();
    } catch (e) { setThresholdError(errorMessage(e)); }
    finally { setBusy(false); }
  }

  function optionList(kind: string) { return entries.filter(e => e.kind === kind && e.status !== "VALIDATION_FAILED" && (kind !== "PORTFOLIO" || !reportingPeriod || e.reportingDate === reportingPeriod)).map(e => <option key={e.id} value={e.id}>{kind === "PORTFOLIO" ? portfolioLabel(e) : `${e.reportingDate || kind} · ${e.id.slice(0, 8)} · ${e.status}`}</option>); }
  const nbfcIds = [...new Set(snapshot?.rows.map(l => l.nbfcId) || [])];
  async function loadTrend() {
    setTrendLoading(true); setTrendError(""); setTrend(null);
    try {
      const current = await api<Quality>("/api/portfolios/quality/" + qualityCurrent);
      const prior = qualityPrior ? await api<Quality>("/api/portfolios/quality/" + qualityPrior) : null;
      if (prior && (prior.nbfcId !== current.nbfcId || prior.reportingDate >= current.reportingDate)) throw { message: "Select an earlier snapshot for the same NBFC, or choose No comparison." };
      setTrend({ current, prior });
    } catch (e) { setTrendError(errorMessage(e)); }
    finally { setTrendLoading(false); }
  }
  const activeSnapshot = snapshot?.id === selected ? snapshot : null;
  const activeAnalysis = analysis?.snapshotId === selected ? analysis : null;
  const openRun = (run: Run) => { setCurrentRun(run); navigate("history"); };
  useEffect(()=>{if(view==="history" && currentRun) document.getElementById("saved-scenario-result")?.scrollIntoView({block:"start"});},[view,currentRun]);
  return <section className="space-y-5" aria-label="Portfolio workspace">
    <div className="portfolio-toolbar">
      <label className="bank-filter">Bank<select aria-label="Bank"><option>Synthetic Demo Bank (SYN-BANK-001)</option></select></label>
      <label className="period-filter">Reporting period<select aria-label="Reporting period" value={reportingPeriod} onChange={e=>{ const period=e.target.value; setReportingPeriod(period); if(period && snapshot?.reportingDate!==period){setSelected(entries.find(row=>row.kind==="PORTFOLIO" && row.status!=="VALIDATION_FAILED" && row.reportingDate===period)?.id || "");setCurrentRun(null);} }}><option value="">All reporting dates</option>{[...new Set(entries.filter(row=>row.kind==="PORTFOLIO" && row.status!=="VALIDATION_FAILED" && row.reportingDate).map(row=>row.reportingDate!))].sort().reverse().map(period=><option key={period}>{period}</option>)}</select></label>
      <label className="portfolio-filter">Portfolio<select aria-label="Portfolio" className={input} value={selected} onChange={e => { setSelected(e.target.value); setCurrentRun(null); }}><option value="">Select a saved portfolio</option>{optionList("PORTFOLIO")}</select></label>
      <button type="button" className="icon-button" aria-label="Refresh workspace data" onClick={()=>{ setError(""); void refresh(); setRefreshTick(t=>t+1); }}><Icon name="refresh"/></button>
      {reviewer && <button type="button" className="report-button" disabled={!activeSnapshot} onClick={()=>activeSnapshot && exportPortfolioReport(activeSnapshot,runs)}><Icon name="download" size={16}/>Export Report</button>}
    </div>
    {view==="dashboard" && reviewer && <DashboardOverview snapshot={activeSnapshot} analysis={activeAnalysis} runs={runs} runsLoaded={runsLoaded} runsError={runsError} currentRun={currentRun} entries={entries} updated={lastUpdated} navigate={navigate} openRun={openRun} openNbfc={id=>{setAnalysisNbfc(id);navigate("analysis");}} />}
    {(view==="history" || view==="alerts") && reviewer && <ScenarioBrowser key={view} snapshot={activeSnapshot} runs={runs} loaded={runsLoaded} error={runsError} alertsOnly={view==="alerts"} openRun={openRun} />}
    {view==="reports" && <ReportsPanel snapshot={activeSnapshot} runs={runs} reviewer={reviewer} />}
    {view==="guide" && <UserGuide navigate={navigate} contributor={contributor} supervisor={supervisor} />}
    {view==="assessments" && analyst && <AssessmentList />}
    {reviewer && !activeSnapshot && !["dashboard","reports","guide","all"].includes(view) && <p className="empty-state">Select a saved portfolio above to load its analysis. Check any service error below if loading does not finish.</p>}

    <div hidden={!show("data")} className="tool-section">
    {contributor ? <>
      <div className="flex flex-wrap items-end justify-between gap-4 pb-1">
        <div><p className="text-xs font-semibold uppercase tracking-widest text-teal-800">Contributor workspace</p><h2 id="portfolio-title" className="mt-2 text-2xl font-bold tracking-tight text-slate-900">Prepare and share your data</h2><p className="mt-2 text-sm text-slate-500">Upload your portfolio, check it, and save it for analyst and supervisor review.</p></div>
        <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-500">SYN-BANK-001 / INR</span>
      </div>
      <nav aria-label="Contributor workspace" className="flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        {([["upload","Upload portfolio"],["saved","Saved portfolios"],["support","Supporting data"]] as const).map(([view,label])=><button key={view} type="button" aria-pressed={contributorView===view} disabled={busy} onClick={()=>setContributorView(view)} className={"rounded-lg px-4 py-2.5 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50 "+(contributorView===view?"bg-teal-800 text-white":"bg-white text-slate-600 hover:bg-slate-100")}>{label}{view==="saved"&&entries.some(e=>e.kind==="PORTFOLIO"&&e.status!=="VALIDATION_FAILED")&&<span className="ml-2 rounded-full bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">{entries.filter(e=>e.kind==="PORTFOLIO"&&e.status!=="VALIDATION_FAILED").length}</span>}</button>)}
      </nav>
    </> : <h2 id="portfolio-title" className="sr-only">Portfolio risk workbench</h2>}
    </div>
    {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-900">{error}</p>}
    {notice && <p role="status" className="rounded bg-teal-50 p-3 text-teal-900">{notice}</p>}
    <div hidden={!show("data")} className="tool-section">
    {contributor && <div hidden={contributorView!=="upload"}>
      <ContributorUpload portfolioName={portfolioName} onPortfolioName={setPortfolioName} file={file} synthetic={synthetic} busy={busy} validation={validation}
        onFile={next=>{setFile(next);setValidation(null);setError("");setNotice("");}}
        onSynthetic={setSynthetic} onValidate={event=>void validateFile(event)} onSave={()=>void saveFile()} />
    </div>}
    </div>
    <div hidden={!show("exposure", "data")} className="tool-section">
    <div hidden={contributor && contributorView!=="saved"} className={box}>
      <div className="flex items-center justify-between gap-3"><div><h3 className="font-bold">Saved portfolios</h3><p className="mt-1 text-sm text-slate-500">Select a saved upload to review the data shared with the risk team.</p></div><Button variant="outline" onClick={() => void refresh()}>Refresh</Button></div>

      {!snapshot ? <p className="mt-3 text-sm text-slate-500">Upload and save a portfolio to begin.</p> : <>
        <p className="my-3 break-words text-sm">{portfolioDetails(snapshot)} · {snapshot.bankId} · {snapshot.reportingDate} · {snapshot.status} · {snapshot.rows.length} loans · {nbfcIds.length} NBFCs</p>
        {snapshot.warnings.length > 0 && <details className="mb-4 rounded bg-amber-50 p-3"><summary className="cursor-pointer">Data reconciliation warnings ({snapshot.warnings.length})</summary>{snapshot.warnings.map((w, i) => <p className="mt-2 text-sm" key={i}>Line {w.line} · {w.field}: {w.message}</p>)}</details>}
        <div className="overflow-auto"><table className="w-full text-left text-sm"><caption className="sr-only">Reported bank loan values from this immutable portfolio</caption><thead><tr><th>NBFC / loan</th><th>Amount lent</th><th>Outstanding</th><th>Explicit EAD</th><th>DPD / status</th></tr></thead><tbody>{snapshot.rows.map(l => <tr key={l.loanId} className="border-t"><td className="py-3">{l.nbfcId}<br />{l.loanId}</td><td>{rupees(l.amountLent)}</td><td>{rupees(l.outstandingAmount)}</td><td>{rupees(l.eadAmount)}</td><td>{l.dpdDays} / {l.loanStatus}</td></tr>)}</tbody></table></div>
        <p className="mt-3 text-xs text-slate-500">Loan DPD describes these bank-held loans. It is not the NBFC's own borrower-portfolio asset quality. Row-level reported revenue is preserved and is not summed as company revenue.</p>
      </>}
    </div>
    </div>
    <div hidden={!show("analysis")} className="tool-section">
    {reviewer && snapshot && snapshot.id === selected && <AnalysisWorkspace initialNbfc={analysisNbfc} key={"analysis-" + snapshot.id} snapshot={snapshot} entries={entries} runs={runs} runsLoaded={runsLoaded} runsError={runsError} />}
    </div>
    <div hidden={!show("exposure")} className="tool-section">
    {reviewer && <PortfolioExposureChart selected={!!selected}
      analysis={analysis?.snapshotId === selected ? analysis : null}
      portfolio={snapshot?.id === selected ? snapshot : null} />}
    {reviewer && analysis && analysis.snapshotId === selected && <div className={box}>
      <h3 className="text-lg font-bold">Selected portfolio analytics</h3>
      <PortfolioIndicators totalEad={analysis.totalEad} nbfcs={analysis.nbfcs} snapshotId={analysis.snapshotId} reportingDate={snapshot?.reportingDate} name={snapshot?.name} scenario={currentRun} />
      <p className="mb-3 text-sm text-slate-600">Total lent: {rupees(analysis.totalLent)} · Reported outstanding: {rupees(analysis.totalOutstanding)}</p>
      <p className="text-sm">{analysis.concentrationDenominator}</p>
      <div className="my-4 h-64"><ResponsiveContainer width="100%" height="100%"><BarChart data={analysis.nbfcs.map(n => ({ nbfcId: n.nbfcId, outstanding: Number(n.outstandingAmount), outstandingExact: n.outstandingAmount }))}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="nbfcId" /><YAxis tickFormatter={n => (Number(n) / 10000000).toFixed(1) + " Cr"} /><Tooltip formatter={(_value,_name,item)=>rupees((item.payload as {outstandingExact:string}).outstandingExact)} /><Bar dataKey="outstanding" name="Reported outstanding INR" fill="#0f766e" /></BarChart></ResponsiveContainer></div>
      <div className="overflow-auto"><table className="w-full text-left text-sm"><thead><tr><th>NBFC</th><th>Loans</th><th>Outstanding</th><th>EAD</th><th>Share of explicit EAD</th></tr></thead><tbody>{analysis.nbfcs.map(n => <tr className="border-t" key={n.nbfcId}><td className="py-3">{n.nbfcId}</td><td>{n.loans}</td><td>{rupees(n.outstandingAmount)}</td><td>{rupees(n.eadAmount)}</td><td>{n.portfolioSharePct == null ? "Unavailable" : n.portfolioSharePct + "%"}</td></tr>)}</tbody></table></div>
    </div>}
    </div>
    <div hidden={!show("data")} className="tool-section">
    {contributor && <div hidden={contributorView!=="support"} className="space-y-4">
      <div><h3 className="text-lg font-semibold">Supporting data</h3><p className="mt-1 text-sm text-slate-500">Add these inputs when preparing a stress analysis. Your portfolio upload is saved separately.</p></div>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Supporting data type">{([["capital","Bank capital"],["quality","NBFC asset quality"]] as const).map(([view,label])=><button type="button" key={view} aria-pressed={supportView===view} disabled={busy} onClick={()=>setSupportView(view)} className={"rounded-lg border px-4 py-2 text-sm font-medium "+(supportView===view?"border-teal-700 bg-teal-50 text-teal-900":"border-slate-200 bg-white text-slate-600")}>{label}</button>)}</div>
      <form hidden={supportView!=="capital"} className={box+" max-w-2xl"} onSubmit={e => { e.preventDefault(); void perform(() => post("/api/portfolios/capital", { bankId: "SYN-BANK-001", reportingDate: date, eligibleCapital: capital, label: capitalLabel }), "Capital version saved."); }}>
        <h3 className="font-bold">Bank capital</h3><p className="mt-2 text-sm text-slate-500">Enter the eligible capital and explain which capital measure it represents.</p>
        <label className="mt-3 block">Reporting date<input className={input} type="date" required value={date} onChange={e => setDate(e.target.value)} /></label>
        <label className="mt-3 block">Eligible capital (INR rupees)<input className={input} required value={capital} onChange={e => setCapital(e.target.value)} /></label>
        <label className="mt-3 block">Capital definition / denominator label<input className={input} required maxLength={200} value={capitalLabel} onChange={e => setCapitalLabel(e.target.value)} /></label>
        <Button className="mt-3" disabled={busy}>Save capital version</Button>
      </form>
      <form hidden={supportView!=="quality"} className={box+" max-w-2xl"} onSubmit={e => { e.preventDefault(); void perform(() => post("/api/portfolios/quality", { bankId: "SYN-BANK-001", nbfcId: qualityNbfc, reportingDate: date, buckets: qualityAmounts, totalAmount: qualityTotal }), "Asset-quality version saved."); }}>
        <h3 className="font-bold">NBFC asset quality</h3><p className="mt-2 text-xs">Enter mutually exclusive synthetic buckets in INR rupees. Do not infer these from the bank loan CSV.</p>
        <label className="mt-3 block">NBFC ID<input className={input} required value={qualityNbfc} onChange={e => setQualityNbfc(e.target.value)} /></label>
        <label className="mt-3 block">Reporting date<input className={input} type="date" required value={date} onChange={e => setDate(e.target.value)} /></label>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">{buckets.map(b => <label key={b} className="text-xs">{b}<input className={input} required value={qualityAmounts[b] || ""} onChange={e => setQualityAmounts({ ...qualityAmounts, [b]: e.target.value })} /></label>)}</div>
        <label className="mt-3 block">Total bucket amount<input className={input} required value={qualityTotal} onChange={e => setQualityTotal(e.target.value)} /></label>
        <Button className="mt-3" disabled={busy}>Save asset-quality version</Button>
      </form>
    </div>}
    </div>
    <div hidden={!show("thresholds")} className="tool-section">
    {supervisor && <form aria-label="Prototype capital-impact alert thresholds" className={box} onSubmit={e => void saveThreshold(e)}>
      <h3 className="font-bold">Prototype capital-impact alert thresholds</h3><p className="mt-2 text-sm">No regulatory defaults are assumed. LOW is below MEDIUM; HIGH begins at the HIGH threshold. These are versioned prototype settings, not RBI limits.</p>
      <fieldset disabled={busy}>
        <div className="my-3 grid gap-3 sm:grid-cols-3"><label>Version name<input className={input} required maxLength={120} value={thresholdName} onChange={e => setThresholdName(e.target.value)} /></label><label>MEDIUM starts at (%)<input className={input} required value={medium} onChange={e => setMedium(e.target.value)} /></label><label>HIGH starts at (%)<input className={input} required value={high} onChange={e => setHigh(e.target.value)} /></label></div>
        <Button disabled={busy}>Save threshold version</Button>
      </fieldset>
      {thresholdError && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-900">Threshold version was not saved: {thresholdError}</p>}
      {savedThreshold && <div role="status" className="mt-4 rounded-lg border border-teal-200 bg-teal-50 p-4 text-sm text-teal-950">
        <p className="font-semibold">Threshold version saved successfully</p>
        <p className="mt-2 break-words">Last saved: <strong>{savedThreshold.name}</strong></p>
        <p className="mt-1 break-all text-xs">Version ID: {savedThreshold.id}</p>
        <ul className="mt-3 list-disc space-y-1 pl-5"><li>LOW: below {savedThreshold.mediumPct}% capital impact</li><li>MEDIUM: {savedThreshold.mediumPct}% to below {savedThreshold.highPct}%</li><li>HIGH: {savedThreshold.highPct}% or above</li></ul>
        <p className="mt-3 text-xs">Available for selection in stress scenarios. Existing scenarios retain their recorded threshold version. Editing the fields above does not change this saved version; save again to create a new version.</p>
      </div>}
    </form>}
    </div>
    <div hidden={!show("analysis", "stress")} className="tool-section">
    {reviewer && <div className={box}>
      <h3 className="font-bold">Approved NBFC asset-quality context</h3>
      <div className="my-3 grid gap-3 sm:grid-cols-2"><label>Current snapshot<select className={input} disabled={trendLoading} value={qualityCurrent} onChange={e => { setQualityCurrent(e.target.value); setQualityPrior(""); setTrend(null); setTrendError(""); }}><option value="">Select</option>{optionList("QUALITY")}</select></label><label>Prior period (optional)<select className={input} disabled={trendLoading || !qualityCurrent} value={qualityPrior} onChange={e => { setQualityPrior(e.target.value); setTrend(null); setTrendError(""); }}><option value="">No comparison</option>{entries.filter(e => e.kind === "QUALITY" && e.status === "APPROVED").map(e => <option key={e.id} value={e.id} disabled={!e.reportingDate || e.reportingDate >= (entries.find(c => c.id === qualityCurrent)?.reportingDate || "")}>{e.reportingDate} · {e.id.slice(0, 8)} · {e.status}</option>)}</select></label></div>
      <p className="mb-3 text-sm text-slate-500">Choose No comparison to view current amounts only. A prior snapshot must be earlier and belong to the same NBFC.</p>
      <Button disabled={busy || trendLoading || !qualityCurrent} onClick={() => void loadTrend()}>{trendLoading ? "Loading asset-quality trend…" : "View asset-quality trend"}</Button>
      {trendError && <p role="alert" className="mt-3 rounded bg-red-50 p-3 text-red-900">{trendError}</p>}
      {trend && <div className="mt-4"><DoughnutChart title="NBFC asset-quality mix" subtitle={trend.current.nbfcId + " · " + trend.current.reportingDate + " · Version " + qualityCurrent.slice(0, 8) + " · Current snapshot only"} slices={buckets.map(name => ({ name, amount: moneyCents(trend.current.buckets[name]) }))} emptyMessage="All asset-quality buckets are zero; no distribution to display." /></div>}
      {trend && <NpaHistoryChart bankId={trend.current.bankId} nbfcId={trend.current.nbfcId} entries={entries} />}
      {trend && <div role="region" aria-label="Asset-quality trend results" className="mt-4 overflow-auto"><p>{trend.current.nbfcId} · {trend.current.reportingDate}{trend.prior ? " vs " + trend.prior.reportingDate : ""}</p><table className="mt-3 w-full text-left text-sm"><thead><tr><th>Bucket</th><th>Current</th><th>Prior</th><th>Change INR</th></tr></thead><tbody>{buckets.map(b => <tr className="border-t" key={b}><td className="py-2">{b}</td><td>{rupees(trend.current.buckets[b])}</td><td>{trend.prior ? rupees(trend.prior.buckets[b]) : "Not supplied"}</td><td>{trend.prior ? delta(trend.current.buckets[b], trend.prior.buckets[b]) : "Not supplied"}</td></tr>)}</tbody></table></div>}
    </div>}
    </div>
    <div hidden={!show("stress")} className="tool-section">
    {reviewer && snapshot && snapshot.id === selected && <StressHeatmap key={snapshot.id} snapshot={snapshot} entries={entries} />}
    </div>
    <div hidden={!show("stress")} className="tool-section">
    {analyst && <form className={box} onSubmit={e => {
      e.preventDefault();
      void perform(async () => { const run = await api<Run>("/api/scenarios", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ snapshotId: selected, nbfcId: nbfc, capitalId, qualityId, thresholdId, severityPct: severity, recoveryPct: recovery, rationale, idempotencyKey: crypto.randomUUID() }) }); setCurrentRun(run); setRuns(old => [run, ...old]); }, "Scenario stored. Review the result or failure reason below.");
    }}>
      <h3 className="text-lg font-bold">Run one-NBFC stress scenario</h3>
      <p className="mt-2 text-sm">Requires approved explicit EAD, matching capital and asset-quality dates, rationale and a saved threshold version. DPD never sets severity automatically.</p>
      <div className="my-3 grid gap-3 sm:grid-cols-2">
        <label>NBFC<select className={input} required value={nbfc} onChange={e => setNbfc(e.target.value)}><option value="">Select</option>{nbfcIds.map(id => <option key={id}>{id}</option>)}</select></label>
        <label>Eligible capital version<select className={input} required value={capitalId} onChange={e => setCapitalId(e.target.value)}><option value="">Select</option>{optionList("CAPITAL")}</select></label>
        <label>Asset-quality version<select className={input} required value={qualityId} onChange={e => setQualityId(e.target.value)}><option value="">Select</option>{optionList("QUALITY")}</select></label>
        <label>Threshold version<select className={input} required value={thresholdId} onChange={e => setThresholdId(e.target.value)}><option value="">Select</option>{optionList("THRESHOLDS")}</select></label>
        <label>Severity (%)<input className={input} type="number" min="0" max="100" step="0.01" required value={severity} onChange={e => setSeverity(e.target.value)} /></label>
        <label>Recovery (%)<input className={input} type="number" min="0" max="100" step="0.01" required value={recovery} onChange={e => setRecovery(e.target.value)} /></label>
      </div>
      <label>Scenario rationale<textarea className={input} maxLength={2000} required value={rationale} onChange={e => setRationale(e.target.value)} /></label>
      <Button className="mt-3" disabled={busy || !snapshot}>Calculate and save scenario</Button>
    </form>}
    </div>
    <div hidden={!show("history", "stress", "assessments")} className="tool-section">
    {reviewer && <div className={box}>
      <h3 id="saved-scenario-result" className="font-bold scroll-mt-6">Saved scenarios and explainable alerts</h3>
      <select aria-label="Saved scenario" className={input} value={currentRun?.id || ""} onChange={e => setCurrentRun(runs.find(r => r.id === e.target.value) || null)}><option value="">Select a scenario</option>{runs.map(r => <option key={r.id} value={r.id}>{r.nbfcId} · {r.status} · {r.id.slice(0, 8)}</option>)}</select>
      {currentRun && <div className="mt-4 space-y-3"><p>{currentRun.status} · Portfolio {portfolioDetails(entries.find(e => e.id === currentRun.snapshotId) || { id: currentRun.snapshotId })}</p>
        {currentRun.failureReason && <p role="alert" className="rounded bg-amber-50 p-3">{currentRun.failureReason} No final alert published.</p>}
        {currentRun.result && <><div className="grid gap-3 sm:grid-cols-3"><p>Direct loss<br /><strong>{rupees(currentRun.result.stressedLoss)}</strong></p><p>Capital impact<br /><strong>{currentRun.result.capitalImpactPct}%</strong></p><p>Prototype alert<br /><strong>{currentRun.result.alertSeverity}</strong></p></div>
          <p>EAD {rupees(currentRun.result.eadAmount)} · {currentRun.result.capitalLabel}: {rupees(currentRun.result.eligibleCapital)} · EAD / capital: {currentRun.result.concentrationCapitalPct}%</p>
          <p className="text-sm">{currentRun.result.formula}</p><p className="text-xs">Threshold version: {currentRun.result.thresholdVersion} · MEDIUM ≥ {currentRun.result.mediumPct}% · HIGH ≥ {currentRun.result.highPct}%</p>
          {analyst && <div><label>Assessment note<textarea className={input} maxLength={2000} value={assessmentNote} onChange={e => setAssessmentNote(e.target.value)} /></label><Button className="mt-3" disabled={busy || !assessmentNote.trim()} onClick={() => void perform(() => post("/api/governance/scenario-assessments", { scenarioId: currentRun.id, note: assessmentNote }), "Assessment submitted to the supervisor queue.")}>Submit scenario for supervisor review</Button></div>}
        </>}
      </div>}
    </div>}
    </div>
  </section>;
}
