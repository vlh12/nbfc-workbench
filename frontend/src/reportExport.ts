import { requireReportApproval, type ReportReview } from "./reportReview";
import type { AnalysisPortfolio, AnalysisScenario } from "./analysisData";
export function downloadText(filename: string, text: string, type: string) {
 const url = URL.createObjectURL(new Blob([text], { type })); const a = document.createElement("a"); a.href=url; a.download=filename; document.body.append(a); a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(url),1000);
}
export function csv(rows: unknown[][]) { return "\uFEFF" + rows.map(row=>row.map(value=>{ let s=String(value ?? ""); if (/^[=+@\-\t\r]/.test(s)) s="'"+s; return '"'+s.replaceAll('"','""')+'"'; }).join(",")).join("\r\n"); }
export function exportPortfolioEvidence(snapshot: AnalysisPortfolio, runs: AnalysisScenario[]) {
 const scoped=runs.filter(r=>r.snapshotId===snapshot.id && r.bankId===snapshot.bankId);
 downloadText("nbfc-report-"+snapshot.reportingDate+".json",JSON.stringify({ report:"NBFC synthetic portfolio evidence", exportedAt:new Date().toISOString(), note:"Saved scenarios are alternative assumptions. Losses must not be added together. Scenario list covers the latest loaded runs, not a complete archive.", portfolio:snapshot, scenarios:scoped },null,2),"application/json");
}

export async function exportPortfolioReport(snapshot: AnalysisPortfolio, runs: AnalysisScenario[], review: ReportReview = {state:"not_loaded"}) {
 requireReportApproval(snapshot, runs, review);
 const { createPortfolioPdf } = await import("./pdfReport");
 const bytes = await createPortfolioPdf(snapshot, runs, review);
 const url = URL.createObjectURL(new Blob([new Uint8Array(bytes)], { type: "application/pdf" }));
 const a = document.createElement("a");
 a.href = url; a.download = "nbfc-report-" + snapshot.reportingDate + ".pdf";
 document.body.append(a); a.click(); a.remove();
 setTimeout(() => URL.revokeObjectURL(url), 1000);
}
