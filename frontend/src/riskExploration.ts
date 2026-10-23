import { moneyCents } from "./dashboardMetrics";
export type QualityVersion = { id: string; createdAt: string; bankId: string; nbfcId: string; reportingDate: string; buckets: Record<string, string>; totalAmount: string };
export function roundRatio(numerator: bigint, denominator: bigint): bigint {
  if (denominator <= 0n) throw new Error("A positive denominator is required.");
  return (numerator + denominator / 2n) / denominator;
}
export function fixedDecimal(value: bigint, places: number): string {
  const scale = 10n ** BigInt(places);
  return (value / scale).toString() + "." + (value % scale).toString().padStart(places, "0");
}
export function stressPreview(ead: string, capital: string, severity: number, recovery: number, medium: string, high: string) {
  const e = moneyCents(ead), c = moneyCents(capital), m = moneyCents(medium), h = moneyCents(high);
  if (e <= 0n || c <= 0n) throw new Error("Explicit EAD and eligible capital must be positive.");
  if (m <= 0n || h <= m) throw new Error("Require 0 < MEDIUM threshold < HIGH threshold.");
  if (![severity, recovery].every(n => Number.isInteger(n) && n >= 0 && n <= 100)) throw new Error("Use whole percentages from 0 to 100.");
  const rawLoss = e * BigInt(severity) * BigInt(100 - recovery);
  // Match the existing model: raw loss to impact at 12 decimals, then display at 6.
  const impact12 = roundRatio(rawLoss * 10n ** 12n, 100n * c);
  const alert = impact12 >= h * 10n ** 10n ? "HIGH" : impact12 >= m * 10n ** 10n ? "MEDIUM" : "LOW";
  return { severity, recovery, lossCents: roundRatio(rawLoss, 10000n), impact: fixedDecimal(roundRatio(impact12, 1000000n), 6), alert };
}
export function latestNpaPeriods(versions: QualityVersion[], bankId: string, nbfcId: string) {
  const latest = new Map<string, QualityVersion>();
  const instantKey = (value: string) => value.replace(/(?:\.(\d+))?Z$/, (_match, fraction: string | undefined) => "." + (fraction || "").padEnd(9, "0") + "Z");
  for (const version of versions.filter(v => v.bankId === bankId && v.nbfcId === nbfcId)) {
    const previous = latest.get(version.reportingDate);
    if (!previous || instantKey(version.createdAt) > instantKey(previous.createdAt) || (instantKey(version.createdAt) === instantKey(previous.createdAt) && version.id > previous.id)) latest.set(version.reportingDate, version);
  }
  return [...latest.values()].sort((a, b) => a.reportingDate.localeCompare(b.reportingDate)).map(version => {
    const total = moneyCents(version.totalAmount), npa = moneyCents(version.buckets.NPA_90_PLUS);
    const percentage = total > 0n ? fixedDecimal(roundRatio(npa * 100000000n, total), 6) : null;
    return { ...version, npa, total, percentage, chartPercentage: percentage == null ? null : Number(percentage) };
  });
}
