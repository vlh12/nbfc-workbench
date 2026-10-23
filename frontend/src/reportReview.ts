import type { AnalysisPortfolio, AnalysisScenario } from "./analysisData";
export type ReviewAssessment = {
  id: string; scenarioId: string; snapshotId: string; submittedBy: string; submittedAt: string;
  submissionNote: string | null; status: string; decidedBy: string | null; decidedAt: string | null; decisionNote: string | null;
};
export type ReviewFollowUp = {
  id: string; assessmentId: string; title: string; owner: string; dueDate: string; status: string;
  createdAt: string; updatedAt: string; evidence?: string | null;
};
export type ReviewEvent = { id: string; entityType: string; entityId: string; action: string; actor: string; createdAt: string; details?: string | null };
export type ReviewHistory = { scenarioId: string; assessments: ReviewAssessment[]; followUps: ReviewFollowUp[]; events: ReviewEvent[] };
export type ReportReview =
  | { state: "loaded"; scenarioId: string; fetchedAt: string; inputReviews?: {name:string;kind:string;decision:string;reviewNote:string;reviewedAt:string}[]; history: ReviewHistory }
  | { state: "not_loaded" | "restricted" | "unavailable" };

// Scope the display again at the export boundary. Never attach an approval from another run.
export function scopedReview(review: ReportReview, scenarioId: string, snapshotId: string): ReviewHistory | null {
  if (review.state !== "loaded" || review.scenarioId !== scenarioId || review.history.scenarioId !== scenarioId) return null;
  const assessments=review.history.assessments.filter(item=>item.scenarioId===scenarioId && item.snapshotId===snapshotId);
  const ids=new Set(assessments.map(item=>item.id));
  const followUps=review.history.followUps.filter(item=>ids.has(item.assessmentId));
  const actionIds=new Set(followUps.map(item=>item.id));
  const events=review.history.events.filter(item=>item.entityType==="ASSESSMENT"?ids.has(item.entityId):item.entityType==="FOLLOW_UP"&&actionIds.has(item.entityId));
  return { scenarioId, assessments, followUps, events };
}

export class ReportApprovalError extends Error {}
export function reportScenario(snapshot: AnalysisPortfolio, runs: AnalysisScenario[]) {
  return runs.filter(run=>run.snapshotId===snapshot.id && run.bankId===snapshot.bankId && run.status==="COMPLETED" && run.result)
    .sort((a,b)=>b.createdAt.localeCompare(a.createdAt)||b.id.localeCompare(a.id))[0];
}
export function requireReportApproval(snapshot: AnalysisPortfolio, runs: AnalysisScenario[], review: ReportReview): AnalysisScenario {
  const run=reportScenario(snapshot,runs);
  if(!run)throw new ReportApprovalError("Complete a scenario and obtain Supervisor approval before downloading the PDF.");
  const history=scopedReview(review,run.id,snapshot.id);
  if(!history)throw new ReportApprovalError("Supervisor approval could not be verified. Sign in as Supervisor and refresh approval status.");
  const latest=history.assessments.slice().sort((a,b)=>Date.parse(b.submittedAt)-Date.parse(a.submittedAt)||b.id.localeCompare(a.id))[0];
  if(!latest)throw new ReportApprovalError("Submit this scenario to the Supervisor before downloading the PDF.");
  if(latest.status!=="APPROVED" || !latest.decidedBy || !latest.decidedAt)
    throw new ReportApprovalError(latest.status==="RETURNED" ? "The Supervisor returned this assessment. Correct and resubmit it, then obtain approval before downloading the PDF." : "Awaiting Supervisor approval. The PDF will be available after approval.");
  return run;
}
