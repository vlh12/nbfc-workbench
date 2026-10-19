import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, auth } from "./auth";
import { Button } from "./components/ui/button";

type Assessment = { id:string; title:string; summary:string; status:"SUBMITTED"|"RETURNED"|"APPROVED"; submittedBy:string; submittedAt:string; decisionNote?:string };
type FollowUpAction = { id:string; assessmentId:string; title:string; ownerName:string; dueDate?:string; status:"OPEN"|"IN_PROGRESS"|"COMPLETED" };
type AuditEvent = { id:string; action:string; actor:string; detail:string; occurredAt:string };
type Problem = { message?:string };
const statusText: Record<string,string> = {SUBMITTED:"Awaiting review",RETURNED:"Returned for update",APPROVED:"Approved",OPEN:"Open",IN_PROGRESS:"In progress",COMPLETED:"Completed"};
function Status({value}:{value:string}) { return <span className="rounded bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-800">{statusText[value] ?? value}</span>; }
function dateTime(value?:string) { return value ? new Date(value).toLocaleString("en-IN") : "—"; }

export function SupervisorWorkbench() {
  const supervisor=auth.hasRealmRole("SUPERVISOR");
  const analyst=auth.hasRealmRole("ANALYST");
  const [assessments,setAssessments]=useState<Assessment[]>([]);
  const [queue,setQueue]=useState<Assessment[]>([]);
  const [actions,setActions]=useState<FollowUpAction[]>([]);
  const [audit,setAudit]=useState<AuditEvent[]>([]);
  const [auditFor,setAuditFor]=useState("");
  const [message,setMessage]=useState("");
  const [problem,setProblem]=useState("");
  const [busy,setBusy]=useState(false);
  const [title,setTitle]=useState("");
  const [summary,setSummary]=useState("");
  const [notes,setNotes]=useState<Record<string,string>>({});
  const [resubmits,setResubmits]=useState<Record<string,string>>({});
  const [actionTitle,setActionTitle]=useState<Record<string,string>>({});
  const [actionOwner,setActionOwner]=useState<Record<string,string>>({});
  const [actionDue,setActionDue]=useState<Record<string,string>>({});

  const refresh=useCallback(async()=>{
    try {
      const assessmentsResponse=await api<Assessment[]>("/api/governance/assessments");
      setAssessments(assessmentsResponse);
      if(supervisor) {
        const responses=await Promise.all([api<Assessment[]>("/api/governance/assessments/queue"),api<FollowUpAction[]>("/api/governance/actions")]);
        setQueue(responses[0]); setActions(responses[1]);
      }
      setProblem("");
    } catch(error) { setProblem((error as Problem)?.message ?? "Governance data could not be loaded."); }
  },[supervisor]);
  useEffect(()=>{ void refresh(); },[refresh]);
  async function perform(request:()=>Promise<unknown>,success:string) {
    setBusy(true); setMessage(""); setProblem("");
    try { await request(); setMessage(success); await refresh(); }
    catch(error) { setProblem((error as Problem)?.message ?? "The requested action could not be completed."); }
    finally { setBusy(false); }
  }
  async function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await perform(()=>api("/api/governance/assessments",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({title,summary})}),"Assessment submitted to supervisor review.");
    setTitle(""); setSummary("");
  }
  async function auditHistory(id:string) {
    try { setAudit(await api<AuditEvent[]>("/api/governance/assessments/"+id+"/audit")); setAuditFor(id); }
    catch(error) { setProblem((error as Problem)?.message ?? "Audit history could not be loaded."); }
  }
  if(!supervisor && !analyst) return null;
  return <section className="space-y-6">
    <div><h2 className="text-2xl font-bold">{supervisor ? "Supervisor review & audit" : "Risk assessment submission"}</h2><p className="mt-1 text-slate-600">{supervisor ? "Review assessments, manage follow-up actions and inspect the audit trail." : "Submit assessments and track the supervisor decision."}</p></div>
    {message && <p role="status" className="rounded-lg bg-teal-50 p-4 text-teal-900">{message}</p>}
    {problem && <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-900">{problem}</p>}
    {analyst && <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">Submit assessment</h3><form onSubmit={submit} className="mt-4 grid gap-4"><label className="text-sm font-medium">Title<input required maxLength={160} value={title} onChange={e=>setTitle(e.target.value)} className="mt-2 w-full rounded-lg border border-slate-300 p-3"/></label><label className="text-sm font-medium">Assessment summary<textarea required maxLength={2000} rows={4} value={summary} onChange={e=>setSummary(e.target.value)} className="mt-2 w-full rounded-lg border border-slate-300 p-3"/></label><div><Button disabled={busy}>Submit for review</Button></div></form></section>}
    {supervisor && <section className="rounded-xl border border-slate-200 bg-white p-6"><div className="flex items-center justify-between"><div><h3 className="text-lg font-bold">Review queue</h3><p className="text-sm text-slate-500">{queue.length} assessment{queue.length===1?"":"s"} awaiting review.</p></div><Button variant="outline" onClick={()=>void refresh()} disabled={busy}>Refresh</Button></div>{!queue.length ? <p className="mt-4 text-slate-500">No assessments are waiting for review.</p> : <div className="mt-4 grid gap-4">{queue.map(item=><article key={item.id} className="rounded-lg border border-slate-200 p-4"><div className="flex justify-between gap-3"><div><h4 className="font-bold">{item.title}</h4><p className="mt-1 whitespace-pre-wrap text-sm">{item.summary}</p><p className="mt-2 text-xs text-slate-500">Submitted by {item.submittedBy} · {dateTime(item.submittedAt)}</p></div><Status value={item.status}/></div><label className="mt-4 block text-sm font-medium">Decision note<textarea required rows={2} maxLength={2000} value={notes[item.id] ?? ""} onChange={e=>setNotes({...notes,[item.id]:e.target.value})} className="mt-2 w-full rounded-lg border border-slate-300 p-3"/></label><div className="mt-3 flex flex-wrap gap-3"><Button disabled={busy || !(notes[item.id] ?? "").trim()} onClick={()=>void perform(()=>api("/api/governance/assessments/"+item.id+"/approve",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({note:notes[item.id]})}),"Assessment approved.")}>Approve</Button><Button variant="outline" disabled={busy || !(notes[item.id] ?? "").trim()} onClick={()=>void perform(()=>api("/api/governance/assessments/"+item.id+"/return",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({note:notes[item.id]})}),"Assessment returned to analyst.")}>Return for update</Button><Button variant="outline" onClick={()=>void auditHistory(item.id)}>Audit</Button></div></article>)}</div>}</section>}
    <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">{supervisor ? "All assessments" : "My assessments"}</h3><div className="mt-4 overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="py-3">Assessment</th>{supervisor && <th>Submitted by</th>}<th>Status</th><th>Decision note</th><th></th></tr></thead><tbody>{assessments.map(item=><tr key={item.id} className="border-b border-slate-100 align-top"><td className="py-3 pr-4"><p className="font-medium">{item.title}</p><p className="text-xs text-slate-500">{dateTime(item.submittedAt)}</p></td>{supervisor && <td>{item.submittedBy}</td>}<td><Status value={item.status}/></td><td className="max-w-sm whitespace-pre-wrap pr-4">{item.decisionNote ?? "—"}</td><td><Button variant="outline" onClick={()=>void auditHistory(item.id)}>Audit</Button></td></tr>)}</tbody></table></div>{!assessments.length && <p className="mt-4 text-slate-500">No assessments submitted.</p>}
      {analyst && assessments.filter(item=>item.status==="RETURNED").map(item=><form key={"r-"+item.id} onSubmit={event=>{event.preventDefault();void perform(()=>api("/api/governance/assessments/"+item.id+"/resubmit",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({summary:resubmits[item.id] || item.summary})}),"Assessment resubmitted.");}} className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-4"><p className="font-semibold">{item.title} was returned</p><p className="mt-1 text-sm">{item.decisionNote}</p><label className="mt-3 block text-sm font-medium">Updated summary<textarea required rows={3} maxLength={2000} value={resubmits[item.id] ?? item.summary} onChange={e=>setResubmits({...resubmits,[item.id]:e.target.value})} className="mt-2 w-full rounded-lg border border-amber-300 p-3"/></label><div className="mt-3"><Button disabled={busy}>Resubmit</Button></div></form>)}</section>
    {supervisor && <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">Follow-up actions</h3><div className="mt-4 grid gap-4">{assessments.map(item=><details key={item.id} className="rounded-lg border border-slate-200 p-4"><summary className="cursor-pointer font-semibold">{item.title}</summary><form onSubmit={event=>{event.preventDefault();void perform(()=>api("/api/governance/assessments/"+item.id+"/actions",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({title:actionTitle[item.id],ownerName:actionOwner[item.id],dueDate:actionDue[item.id] || null})}),"Follow-up action created.");}} className="mt-4 grid gap-3 sm:grid-cols-3"><input required placeholder="Action title" maxLength={160} value={actionTitle[item.id] ?? ""} onChange={e=>setActionTitle({...actionTitle,[item.id]:e.target.value})} className="rounded border border-slate-300 p-2"/><input required placeholder="Owner" maxLength={160} value={actionOwner[item.id] ?? ""} onChange={e=>setActionOwner({...actionOwner,[item.id]:e.target.value})} className="rounded border border-slate-300 p-2"/><input type="date" value={actionDue[item.id] ?? ""} onChange={e=>setActionDue({...actionDue,[item.id]:e.target.value})} className="rounded border border-slate-300 p-2"/><div><Button disabled={busy}>Create action</Button></div></form></details>)}</div><div className="mt-6 overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="py-3">Action</th><th>Owner</th><th>Due</th><th>Status</th></tr></thead><tbody>{actions.map(action=><tr key={action.id} className="border-b border-slate-100"><td className="py-3">{action.title}</td><td>{action.ownerName}</td><td>{action.dueDate ?? "—"}</td><td><select aria-label={"Status for "+action.title} value={action.status} disabled={busy || action.status==="COMPLETED"} onChange={e=>void perform(()=>api("/api/governance/actions/"+action.id,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({status:e.target.value})}),"Follow-up status updated.")} className="rounded border border-slate-300 p-2"><option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option></select></td></tr>)}</tbody></table></div>{!actions.length && <p className="mt-4 text-slate-500">No follow-up actions yet.</p>}</section>}
    {auditFor && <section className="rounded-xl border border-slate-200 bg-white p-6"><div className="flex justify-between"><h3 className="text-lg font-bold">Audit history</h3><Button variant="outline" onClick={()=>{setAuditFor("");setAudit([]);}}>Close</Button></div>{!audit.length ? <p className="mt-4 text-slate-500">No audit events found.</p> : <ol className="mt-4 space-y-3 border-l border-slate-200 pl-5">{audit.map(event=><li key={event.id}><p className="font-semibold">{event.action.replaceAll("_"," ")}</p><p className="text-sm">{event.detail}</p><p className="text-xs text-slate-500">{event.actor} · {dateTime(event.occurredAt)}</p></li>)}</ol>}</section>}
  </section>;
}

