import { DoughnutChart, KpiCard } from "./DashboardVisuals";
import { localDateKey, overdueCount } from "../dashboardMetrics";
export function GovernanceIndicators({ assessments, followUps, loaded, error }: {
  assessments: { status: string }[]; followUps: { status: string; dueDate: string }[]; loaded: boolean; error: boolean;
}) {
  const ready = loaded && !error;
  const count = (status: string) => assessments.filter(a => a.status === status).length;
  const value = (amount: number) => ready ? String(amount) : error ? "Unavailable" : "Loading…";
  const today = localDateKey();
  return <div className="space-y-4">
    <p className="text-xs text-slate-500">Loaded governance records: up to 100 latest assessments and 100 follow-ups ordered by due date. Counts cover this loaded set. Overdue uses your local date: {today}.</p>
    <div className="grid gap-3 sm:grid-cols-3">
      <KpiCard label="Pending assessments" value={value(count("SUBMITTED"))} detail="Submitted and awaiting a decision" />
      <KpiCard label="Returned assessments" value={value(count("RETURNED"))} detail="Returned for correction" />
      <KpiCard label="Overdue follow-ups" value={value(overdueCount(followUps, today))} detail="Due before today; excludes COMPLETED and CLOSED" />
    </div>
    {ready ? <DoughnutChart title="Assessment status" subtitle="Loaded assessments · Each assessment counted once by its current status" monetary={false} slices={[{ name: "Submitted", amount: BigInt(count("SUBMITTED")) }, { name: "Returned", amount: BigInt(count("RETURNED")) }, { name: "Approved", amount: BigInt(count("APPROVED")) }]} emptyMessage="No assessments in the loaded records." /> : <p className="text-sm text-slate-500">{error ? "Assessment chart unavailable. Refresh when the governance service is available." : "Loading assessment chart…"}</p>}
  </div>;
}
