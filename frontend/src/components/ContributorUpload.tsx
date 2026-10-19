import { useEffect, useRef, type FormEvent } from "react";
import { Button } from "./ui/button";

type Issue = { line: number; field: string; message: string };
type Validation = { rows: unknown[]; warnings: Issue[]; errors: Issue[] };
type Props = {
  file: File | null;
  synthetic: boolean;
  busy: boolean;
  validation: Validation | null;
  onFile: (file: File | null) => void;
  onSynthetic: (checked: boolean) => void;
  onValidate: (event: FormEvent) => void;
  onSave: () => void;
};

export function ContributorUpload({ file, synthetic, busy, validation, onFile, onSynthetic, onValidate, onSave }: Props) {
  const picker = useRef<HTMLInputElement>(null);
  useEffect(() => { if (!file && picker.current) picker.current.value = ""; }, [file]);
  const valid = !!validation && validation.errors.length === 0 && validation.rows.length > 0;
  const step = valid ? 3 : file ? 2 : 1;

  return <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 px-5 py-5 sm:px-8">
      <h3 className="text-lg font-semibold text-slate-900">Upload your loan portfolio</h3>
      <p className="mt-1 text-sm text-slate-500">Choose a CSV, check the results, then save it for the risk team.</p>
      <ol aria-label="Upload progress" className="mt-5 grid grid-cols-3 gap-2 text-xs sm:text-sm">
        {["Choose file", "Validate", "Save & share"].map((label, i) => <li key={label} aria-current={step === i + 1 ? "step" : undefined} className={"flex items-center gap-2 rounded-lg px-2 py-2 " + (step >= i + 1 ? "bg-teal-50 text-teal-900" : "bg-slate-50 text-slate-400")}>
          <span className={"inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold " + (step >= i + 1 ? "bg-teal-800 text-white" : "bg-slate-200 text-slate-500")}>{i + 1}</span>{label}
        </li>)}
      </ol>
    </div>
    <div className="grid gap-6 p-5 sm:p-8 lg:grid-cols-[minmax(0,1fr)_230px]">
      <form onSubmit={onValidate} className="min-w-0 space-y-4">
        <label className={"block rounded-xl border-2 border-dashed p-6 transition " + (file ? "border-teal-300 bg-teal-50/40" : "border-slate-300 bg-slate-50")}>
          <span className="mb-3 block font-semibold text-slate-800">{file ? "File selected" : "Choose your portfolio CSV"}</span>
          <input ref={picker} type="file" accept=".csv,text/csv" required disabled={busy} onChange={e => onFile(e.target.files?.[0] || null)} className="block w-full min-w-0 text-sm text-slate-600 file:mr-3 file:rounded-lg file:border file:border-slate-200 file:bg-white file:px-3 file:py-2 file:text-sm file:font-semibold file:text-slate-700 hover:file:bg-slate-100" aria-describedby="portfolio-file-help" />
          <span id="portfolio-file-help" className="mt-3 block text-xs text-slate-500">CSV only. Up to 2 MB and 10,000 loan records.</span>
        </label>
        {file && <p className="break-all text-xs text-slate-500">{file.name} ({(file.size / 1024).toFixed(1)} KB)</p>}
        <label className="flex cursor-pointer items-start gap-3 text-sm text-slate-600"><input type="checkbox" checked={synthetic} disabled={busy} required onChange={e => onSynthetic(e.target.checked)} className="mt-0.5 h-4 w-4 accent-teal-800" /><span>I confirm this file contains synthetic data only.</span></label>
        <div className="flex flex-wrap items-center gap-3">
          <Button disabled={busy || !file || !synthetic}>{busy ? "Please wait..." : validation ? "Validate again" : "Validate file"}</Button>
          <span className="text-xs text-slate-500">Nothing is shared until you save.</span>
        </div>
      </form>
      <aside className="rounded-xl bg-slate-50 p-4 text-sm">
        <h4 className="font-semibold text-slate-800">Need a sample?</h4>
        <p className="mt-2 text-xs leading-5 text-slate-500">Use a matching template to prepare your file.</p>
        <a href="/portfolio-loans-19.csv" download className="mt-4 block font-medium text-teal-800 underline underline-offset-4">Download sample CSV</a>
        <a href="/portfolio-loans-ead-template.csv" download className="mt-3 block font-medium text-teal-800 underline underline-offset-4">Download EAD template</a>
        <p className="mt-4 text-xs leading-5 text-slate-500">The sample supports portfolio review. Add explicit EAD values for stress analysis.</p>
      </aside>
    </div>
    {validation && <div aria-live="polite" className="border-t border-slate-100 px-5 py-5 sm:px-8">
      <div className={"rounded-xl border p-4 " + (validation.errors.length ? "border-red-200 bg-red-50" : validation.warnings.length ? "border-amber-200 bg-amber-50" : "border-teal-200 bg-teal-50")}>
        <h4 className="font-semibold">{validation.errors.length ? "Fix the errors and validate again" : validation.warnings.length ? "Ready to save with warnings" : "Your file is ready to save"}</h4>
        <p className="mt-1 text-sm">{validation.errors.length ? validation.errors.length + " errors. No loan rows have been saved." : validation.rows.length + " loan records checked."}{validation.warnings.length > 0 && " " + validation.warnings.length + " warnings to review."}</p>
        {validation.errors.length > 0 && <ul className="mt-3 max-h-56 list-disc space-y-2 overflow-auto pl-5 text-sm">{validation.errors.map((issue, i) => <li key={i}>Line {issue.line} - {issue.field}: {issue.message}</li>)}</ul>}
        {validation.warnings.length > 0 && <details className="mt-3"><summary className="cursor-pointer text-sm font-medium">View warnings ({validation.warnings.length})</summary><ul className="mt-3 max-h-56 list-disc space-y-2 overflow-auto pl-5 text-sm">{validation.warnings.map((issue, i) => <li key={i}>Line {issue.line} - {issue.field}: {issue.message}</li>)}</ul></details>}
      </div>
      {valid && <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><p className="max-w-lg text-sm text-slate-500">Saving makes this portfolio available to the analyst and supervisor.{validation.warnings.length > 0 && " Warnings remain visible and may prevent stress approval."}</p><Button onClick={onSave} disabled={busy}>Save portfolio</Button></div>}
    </div>}
  </div>;
}
