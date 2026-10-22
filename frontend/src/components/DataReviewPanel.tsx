import { useEffect, useState } from "react";
import { api } from "../auth";
import { entryLabel } from "../workflowLabels";
type Entry={id:string;kind:string;name?:string|null;status:string;reportingDate:string|null;createdAt:string};
type Review={id:string;decision:string;reviewNote:string;reviewedAt:string};
type Detail={name?:string;bankId?:string;nbfcId?:string;reportingDate?:string;eligibleCapital?:string;label?:string;totalAmount?:string;buckets?:Record<string,string>;warnings?:{message:string}[];rows?:{loanId:string;nbfcId:string;amountLent:string;outstandingAmount:string;eadAmount:string|null;loanStatus:string}[]};
export function DataReviewPanel({entries,onChanged}:{entries:Entry[];onChanged:()=>void}){
 const [selected,setSelected]=useState(""),[detail,setDetail]=useState<Detail|null>(null),[history,setHistory]=useState<Review[]>([]),[note,setNote]=useState(""),[error,setError]=useState(""),[notice,setNotice]=useState(""),[busy,setBusy]=useState(false);
 const rows=entries.filter(e=>["PORTFOLIO","CAPITAL","QUALITY"].includes(e.kind)&&e.status!=="VALIDATION_FAILED");
 const current=rows.find(e=>e.id===selected);
 useEffect(()=>{let live=true;setDetail(null);setHistory([]);setError("");setNote("");
  if(current){const path=current.kind==="PORTFOLIO"?"/":"/"+current.kind.toLowerCase()+"/";
   void Promise.all([api<Detail>("/api/portfolios"+path+current.id),api<Review[]>("/api/portfolios/"+current.id+"/reviews")]).then(([data,events])=>{if(live){setDetail(data);setHistory(events)}}).catch(()=>{if(live)setError("Data could not be loaded. Refresh and retry.")});}
  return()=>{live=false};
 },[selected,current?.status]);
 async function decide(decision:string){if(!current||!detail||busy)return;setBusy(true);setError("");setNotice("");try{
  await api("/api/portfolios/"+current.id+"/review",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({decision,note})});
  setNotice(decision==="APPROVED"?"Data approved for analyst use.":"Data returned. The contributor must upload a corrected version.");setNote("");onChanged();
 }catch(e){setError(e&&typeof e==="object"&&"message" in e?String(e.message):"Decision could not be saved.")}finally{setBusy(false)}}
 return <section className="dashboard-card space-y-4" aria-label="Supervisor data review">
  <h2 className="text-xl font-bold">1. Review contributor data</h2>
  <p>Check the portfolio, matching bank capital and NBFC asset-quality buckets before approving them for the analyst. This is separate from approving the final scenario.</p>
  <p>{rows.filter(e=>e.status==="PENDING_REVIEW").length} inputs awaiting review</p>
  <label>Data to review<select className="mt-1 w-full rounded border p-3" aria-label="Data to review" value={selected} onChange={e=>{setSelected(e.target.value);setNotice("")}}><option value="">Select a named input</option>{rows.map(e=><option key={e.id} value={e.id}>{entryLabel(e)}</option>)}</select></label>
  {error&&<p role="alert" className="rounded bg-red-50 p-3">{error}</p>}{notice&&<p role="status" className="rounded bg-teal-50 p-3">{notice}</p>}
  {current&&detail&&<>
   <h3 className="font-bold">{current.name} · {current.status.replaceAll("_"," ")}</h3>
   <p>{detail.bankId} · {detail.reportingDate}{detail.nbfcId?" · "+detail.nbfcId:""}</p>
   {detail.eligibleCapital&&<p>Eligible capital: INR {detail.eligibleCapital} · Definition: {detail.label}</p>}
   {detail.buckets&&<><h4 className="font-semibold">Check asset-quality amounts (INR)</h4><table className="w-full text-left"><thead><tr><th>Bucket</th><th>Amount</th></tr></thead><tbody>{Object.entries(detail.buckets).map(([name,amount])=><tr key={name}><td>{name.replaceAll("_"," ")}</td><td>{amount}</td></tr>)}</tbody></table><p>Total: INR {detail.totalAmount}. These describe the NBFC's borrower portfolio, not bank-loan DPD.</p></>}
   {!!detail.warnings?.length&&<div className="bg-amber-50 p-3">{detail.warnings.map((w,i)=><p key={i}>{w.message}</p>)}<p>Return this version for correction before approval.</p></div>}
   {detail.rows&&<div className="table-scroll"><table className="w-full text-left text-sm"><thead><tr><th>Loan / NBFC</th><th>Lent INR</th><th>Outstanding INR</th><th>Explicit EAD INR</th><th>Status</th></tr></thead><tbody>{detail.rows.map(row=><tr key={row.loanId}><td>{row.loanId}<br/>{row.nbfcId}</td><td>{row.amountLent}</td><td>{row.outstandingAmount}</td><td>{row.eadAmount??"Missing"}</td><td>{row.loanStatus}</td></tr>)}</tbody></table></div>}
   {current.status==="PENDING_REVIEW"&&<><label className="block">Review note<textarea aria-label="Data review note" className="mt-1 w-full rounded border p-3" required maxLength={2000} value={note} onChange={e=>setNote(e.target.value)} placeholder="What did you check? If returning, explain what must be corrected."/></label><div className="flex gap-3"><button className="report-button" disabled={busy||!note.trim()||!!detail.warnings?.length||!!detail.rows?.some(row=>row.eadAmount==null)} onClick={()=>void decide("APPROVED")}>Approve data for analyst</button><button className="report-button" disabled={busy||!note.trim()} onClick={()=>void decide("RETURNED")}>Return data for correction</button></div></>}
   {history.map(event=><p key={event.id}>{event.decision} · {new Date(event.reviewedAt).toLocaleString("en-IN")} · {event.reviewNote}</p>)}
  </>}
 </section>;
}
