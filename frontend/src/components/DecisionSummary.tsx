import { moneyCents, moneyLabel } from "../dashboardMetrics";
type Result={stressedLoss:string;capitalImpactPct:string;alertSeverity:string;formula:string};
export function DecisionSummary({name,nbfc,result}:{name:string;nbfc?:string;result:Result}){
 const loss=moneyCents(result.stressedLoss);
 const croreHundredths=(loss+5000000n)/10000000n;
 const crore=(croreHundredths/100n).toLocaleString("en-IN")+"."+(croreHundredths%100n).toString().padStart(2,"0");
 const impact=result.capitalImpactPct.replace(/(\.\d*?[1-9])0+$|\.0+$/,"$1");
 const alert=result.alertSeverity.charAt(0)+result.alertSeverity.slice(1).toLowerCase();
 const tone=result.alertSeverity==="HIGH"?"bg-rose-100 text-rose-900":result.alertSeverity==="MEDIUM"?"bg-amber-100 text-amber-900":"bg-teal-100 text-teal-900";
 return <section aria-label="Scenario decision summary" className="my-4 rounded-xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
  <h4 className="break-words text-base font-semibold text-slate-900">{name}</h4>
  <p className="mt-1 text-sm text-slate-500">{nbfc} · Saved stress scenario</p>
  <dl className="my-4 grid gap-3 sm:grid-cols-2">
   <div className="rounded-lg border border-slate-200 bg-white p-3"><dt className="text-sm text-slate-600">Modelled loss</dt><dd className="mt-1 text-xl font-bold text-slate-900">₹{crore} crore</dd><dd className="mt-1 break-words text-xs text-slate-500">{moneyLabel(loss)} exactly</dd></div>
   <div className="rounded-lg border border-slate-200 bg-white p-3"><dt className="text-sm text-slate-600">Impact on eligible capital</dt><dd className="mt-1 text-xl font-bold text-slate-900">{impact}%</dd></div>
  </dl>
  <p className="flex flex-wrap items-center gap-2 text-sm"><span className="text-slate-600">Prototype alert</span><strong className={"rounded-full px-3 py-1 "+tone}>{alert}</strong></p>
  <p className="mt-3 text-sm leading-6 text-slate-700">Under the selected assumptions, the modelled loss would equal {impact}% of the selected eligible capital. This is a what-if result, not a recorded actual loss.</p>
  <details className="mt-4 border-t border-slate-200 pt-3"><summary className="cursor-pointer text-sm font-semibold text-blue-700">Calculation details</summary><p className="mt-3 break-words text-sm leading-6 text-slate-600">{result.formula}</p><p className="mt-2 text-xs text-slate-500">Alert bands use the saved prototype thresholds. They are not RBI limits.</p></details>
 </section>;
}
