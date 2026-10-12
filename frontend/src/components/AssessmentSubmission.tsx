import { useState } from "react";
import { api } from "../auth";
import { Button } from "./ui/button";
type Batch={id:string;rowCount:number;status:string};
export function AssessmentSubmission({batches}:{batches:Batch[]}) {
 const [batchId,setBatchId]=useState(""),[note,setNote]=useState(""),[message,setMessage]=useState("");
 async function submit(){if(!batchId)return;try{await api("/api/governance/assessments",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({batchId,note})});setMessage("Assessment submitted to the supervisor.");setNote("");}catch{setMessage("Assessment could not be submitted.");}}
 return <section className="rounded-xl border border-slate-200 bg-white p-6"><h3 className="text-lg font-bold">Submit assessment</h3><p className="mt-1 text-sm text-slate-600">Send a validated batch for supervisor review.</p><select value={batchId} onChange={e=>setBatchId(e.target.value)} className="mt-3 w-full rounded-lg border border-slate-300 p-3 text-sm"><option value="">Select a validated batch</option>{batches.map(b=><option key={b.id} value={b.id}>{b.id} · {b.rowCount} records</option>)}</select><textarea value={note} onChange={e=>setNote(e.target.value)} placeholder="Assessment note" maxLength={2000} className="mt-3 min-h-24 w-full rounded-lg border border-slate-300 p-3 text-sm"/><Button className="mt-3" onClick={()=>void submit()} disabled={!batchId}>Submit for review</Button>{message&&<p role="status" className="mt-3 rounded-lg bg-slate-50 p-3 text-sm">{message}</p>}</section>;
}

