import { portfolioDetails } from "../portfolioLabels";
import { DoughnutChart, KpiCard } from "./DashboardVisuals";
import { exposureMetrics, moneyCents, moneyLabel, shareLabel } from "../dashboardMetrics";

type Scenario = { id: string; snapshotId: string; nbfcId: string; status: string; result: { stressedLoss: string; capitalImpactPct: string } | null };
export function PortfolioIndicators({ totalEad, nbfcs, snapshotId, reportingDate, name, scenario }: {
  totalEad: string | null; nbfcs: { nbfcId: string; eadAmount: string | null }[];
  snapshotId: string; reportingDate?: string; name?: string | null; scenario: Scenario | null;
}) {
  const metrics = exposureMetrics(totalEad, nbfcs);
  const selectedScenario = scenario?.snapshotId === snapshotId && scenario.status === "COMPLETED" && scenario.result ? scenario : null;
  const missing = metrics ? "No positive EAD" : "Incomplete EAD";
  const scenarioDetail = selectedScenario ? selectedScenario.nbfcId + " · Run " + selectedScenario.id.slice(0, 8) : "Select a completed scenario for this portfolio below.";
  return <div className="my-4 space-y-4">
    <p className="break-words text-xs text-slate-500">Selected portfolio · {portfolioDetails({ id: snapshotId, name, reportingDate })} · {reportingDate} · INR</p>
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <KpiCard label="Total explicit EAD" value={metrics ? moneyLabel(metrics.total) : missing} detail="Selected portfolio only" />
      <KpiCard label="Largest NBFC share" value={metrics && metrics.total > 0n ? shareLabel(metrics.largest?.amount || 0n, metrics.total) : missing} detail={metrics && metrics.total > 0n ? metrics.largest?.name : "Requires complete positive EAD"} />
      <KpiCard label="Top 3 NBFCs’ share" value={metrics && metrics.total > 0n ? shareLabel(metrics.topThree, metrics.total) : missing} detail="Up to three largest NBFCs by explicit EAD" />
      <KpiCard label="Selected scenario loss" value={selectedScenario ? moneyLabel(moneyCents(selectedScenario.result!.stressedLoss)) : "Not selected"} detail={scenarioDetail} />
      <KpiCard label="Capital impact (%)" value={selectedScenario ? selectedScenario.result!.capitalImpactPct + "%" : "Not selected"} detail={scenarioDetail} />
    </div>
    {metrics ? <DoughnutChart title="EAD share by NBFC" subtitle="Selected portfolio · Five largest NBFCs plus Others · Denominator: total explicit EAD" slices={metrics.slices} emptyMessage="No positive EAD in this portfolio; concentration shares are not available." />
      : <section aria-label="EAD share by NBFC" className="rounded-xl border border-slate-200 p-4"><h4 className="font-semibold">EAD share by NBFC</h4><p className="mt-3 text-sm text-amber-800">Incomplete EAD. Supply explicit EAD for every loan before viewing concentration shares.</p></section>}
  </div>;
}
