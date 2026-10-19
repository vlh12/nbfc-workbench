import { moneyCents, moneyLabel, shareLabel } from "./dashboardMetrics";

export type AnalysisLoan = { loanId: string; nbfcId: string; amountLent: string; outstandingAmount: string; eadAmount: string | null; dpdDays: number; loanStatus: string };
export type AnalysisPortfolio = { id: string; bankId: string; reportingDate: string; status: string; rows: AnalysisLoan[]; warnings: { line: number; field: string; message: string }[] };
export type AnalysisEntry = { id: string; kind: string; reportingDate: string | null; status: string; createdAt: string };
export type AnalysisScenario = {
  id: string; bankId: string; snapshotId: string; nbfcId: string; status: string; createdAt: string;
  modelVersion?: string; failureReason: string | null;
  inputs?: { capitalId: string; qualityId: string; thresholdId: string; severityPct: string; recoveryPct: string; rationale: string };
  result: { eadAmount: string; eligibleCapital: string; capitalLabel: string; severityPct?: string; recoveryPct?: string;
    stressedLoss: string; capitalImpactPct: string; concentrationCapitalPct: string; alertSeverity: string;
    thresholdVersion: string; mediumPct: string; highPct: string; formula: string } | null;
};
export function requestMessage(error: unknown): string {
  return error && typeof error === "object" && "message" in error ? String(error.message) : "Data could not be loaded. Retry when the service is available.";
}
export function sumExposure(rows: AnalysisLoan[]) {
  return { loans: rows.length, outstanding: rows.reduce((sum, row) => sum + moneyCents(row.outstandingAmount), 0n),
    ead: rows.some(row => row.eadAmount == null) ? null : rows.reduce((sum, row) => sum + moneyCents(row.eadAmount!), 0n) };
}
export function signedMoney(value: bigint) { return (value < 0n ? "−" : value > 0n ? "+" : "") + moneyLabel(value < 0n ? -value : value); }
export function periodComparison(prior: AnalysisPortfolio, current: AnalysisPortfolio) {
  if (prior.bankId !== current.bankId) throw new Error("Select portfolio versions from the same bank.");
  if (prior.reportingDate >= current.reportingDate) throw new Error("The prior portfolio must have an earlier reporting date.");
  const ids = [...new Set([...prior.rows, ...current.rows].map(row => row.nbfcId))].sort();
  return ids.map(nbfcId => {
    const before = sumExposure(prior.rows.filter(row => row.nbfcId === nbfcId));
    const after = sumExposure(current.rows.filter(row => row.nbfcId === nbfcId));
    const delta = before.ead == null || after.ead == null ? null : after.ead - before.ead;
    const change = !before.loans ? "New in current snapshot" : !after.loans ? "Absent from current snapshot" : delta == null ? "Incomplete EAD" : delta > 0n ? "Increased" : delta < 0n ? "Reduced" : "Unchanged";
    return { nbfcId, before, after, delta, outstandingDelta: after.outstanding - before.outstanding, change,
      changePct: delta == null ? "Not available" : before.ead === 0n ? "No prior EAD" : (delta < 0n ? "−" : delta > 0n ? "+" : "") + shareLabel(delta < 0n ? -delta : delta, before.ead!) };
  });
}
export function scenarioRate(run: AnalysisScenario, field: "severityPct" | "recoveryPct") { return run.result?.[field] ?? run.inputs?.[field]; }
export function versionWarnings(a: AnalysisScenario, b: AnalysisScenario) {
  const checks: [string, string | undefined, string | undefined][] = [
    ["Portfolio", a.snapshotId, b.snapshotId], ["Capital", a.inputs?.capitalId, b.inputs?.capitalId],
    ["Asset-quality", a.inputs?.qualityId, b.inputs?.qualityId], ["Threshold", a.result?.thresholdVersion, b.result?.thresholdVersion],
    ["Model", a.modelVersion, b.modelVersion],
  ];
  return checks.flatMap(([name, first, second]) => !first || !second ? [name + " version not recorded; comparability cannot be fully verified."] : first !== second ? [name + " versions differ (" + first + " vs " + second + ")."] : []);
}
function percentMicros(value: string) {
  if (!/^\d+(\.\d{1,6})?$/.test(value)) throw new Error("Invalid stored impact percentage.");
  const [whole, fraction = ""] = value.split(".");
  return BigInt(whole) * 1000000n + BigInt(fraction.padEnd(6, "0"));
}
function signedImpact(value: bigint) {
  const abs = value < 0n ? -value : value;
  return (value < 0n ? "−" : value > 0n ? "+" : "") + (abs / 1000000n) + "." + (abs % 1000000n).toString().padStart(6, "0");
}
export function riskDrivers(base: AnalysisScenario, target: AnalysisScenario): string[] {
  if (base.status !== "COMPLETED" || target.status !== "COMPLETED" || !base.result || !target.result) return ["Select two completed saved scenarios with results."];
  if (base.bankId !== target.bankId || base.nbfcId !== target.nbfcId) return ["Compare scenarios for the same bank and NBFC."];
  const a = base.result, b = target.result;
  const lossDelta = moneyCents(b.stressedLoss) - moneyCents(a.stressedLoss);
  const notes = ["Direct loss " + (lossDelta > 0n ? "increased" : lossDelta < 0n ? "decreased" : "was unchanged") + " (" + signedMoney(lossDelta) + ").",
    "Capital impact changed from " + a.capitalImpactPct + "% to " + b.capitalImpactPct + "% (" + signedImpact(percentMicros(b.capitalImpactPct) - percentMicros(a.capitalImpactPct)) + " percentage points)."];
  let lossInputsChanged = 0, unknown = false;
  const fields: [string, string | undefined, string | undefined, boolean][] = [
    ["Severity", scenarioRate(base, "severityPct"), scenarioRate(target, "severityPct"), false],
    ["Recovery", scenarioRate(base, "recoveryPct"), scenarioRate(target, "recoveryPct"), false],
    ["Explicit EAD", a.eadAmount, b.eadAmount, true],
  ];
  for (const [name, first, second, currency] of fields) {
    if (first == null || second == null) { unknown = true; notes.push(name + " was not recorded for both runs."); continue; }
    const left = moneyCents(first), right = moneyCents(second);
    if (left !== right) lossInputsChanged++;
    const display = (s: string) => currency ? moneyLabel(moneyCents(s)) : s + "%";
    notes.push(name + (left === right ? " remained " + display(first) + "." : (right > left ? " increased" : " decreased") + " from " + display(first) + " to " + display(second) + "."));
  }
  const capitalChanged = moneyCents(a.eligibleCapital) !== moneyCents(b.eligibleCapital);
  notes.push(capitalChanged ? "Eligible capital changed from " + moneyLabel(moneyCents(a.eligibleCapital)) + " to " + moneyLabel(moneyCents(b.eligibleCapital)) + "; capital is the impact denominator and does not change direct loss." : "Eligible capital remained " + moneyLabel(moneyCents(a.eligibleCapital)) + ".");
  if (unknown || !base.modelVersion || base.modelVersion !== target.modelVersion) notes.push("Model or input evidence is incomplete or differs; no single-driver attribution is made.");
  else if (lossInputsChanged > 1) notes.push("Multiple loss inputs changed. The recorded loss difference cannot be attributed to one driver alone.");
  else if (lossInputsChanged === 1) notes.push("Only one direct-loss input changed under the same saved model; the other loss inputs were unchanged.");
  else if (lossDelta !== 0n) notes.push("Stored loss differs despite unchanged recorded loss inputs; verify model evidence and rounding before drawing conclusions.");
  else notes.push("Recorded direct-loss inputs and loss were unchanged.");
  if (a.thresholdVersion !== b.thresholdVersion) notes.push("Alert thresholds changed; alert bands use their own recorded versions and are not a like-for-like risk comparison.");
  return notes;
}
