import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { moneyLabel, shareLabel, type AmountSlice } from "../dashboardMetrics";

const colors = ["#0f766e", "#6366f1", "#d97706", "#be185d", "#0284c7", "#7c3aed", "#64748b"];
export function KpiCard({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return <div role="group" aria-label={label} className="min-w-0 rounded-xl border border-slate-200 bg-slate-50 p-4">
    <p className="text-sm text-slate-600">{label}</p><p className="mt-2 break-words text-xl font-bold text-slate-900">{value}</p>
    {detail && <p className="mt-2 break-words text-xs text-slate-500">{detail}</p>}
  </div>;
}
export function DoughnutChart({ title, subtitle, slices, monetary = true, emptyMessage = "No positive amounts to display." }: {
  title: string; subtitle: string; slices: AmountSlice[]; monetary?: boolean; emptyMessage?: string;
}) {
  const total = slices.reduce((sum, slice) => sum + slice.amount, 0n);
  const format = (amount: bigint) => monetary ? moneyLabel(amount) : amount.toLocaleString("en-IN");
  // Only chart geometry uses approximate numbers; displayed amounts and percentages use integer arithmetic.
  const data = slices.map((slice, index) => ({ ...slice, weight: total > 0n ? Number(slice.amount * 100000000n / total) / 1000000 : 0, color: colors[index % colors.length] }));
  return <section aria-label={title} className="min-w-0 rounded-xl border border-slate-200 bg-white p-4">
    <h4 className="font-semibold text-slate-900">{title}</h4><p className="mt-1 break-words text-xs text-slate-500">{subtitle}</p>
    {total <= 0n ? <p className="py-8 text-center text-sm text-slate-500">{emptyMessage}</p> : <>
      <div className="relative mx-auto h-64 w-full max-w-sm" aria-label={title + "; exact values in the table below"}>
        <ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={data} dataKey="weight" nameKey="name" innerRadius="62%" outerRadius="86%" isAnimationActive={false}>
          {data.map(slice => <Cell key={slice.name} fill={slice.color} />)}
        </Pie><Tooltip formatter={(_value, _name, item) => { const slice = item.payload as typeof data[number]; return [format(slice.amount) + " · " + shareLabel(slice.amount, total), slice.name]; }} /></PieChart></ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center px-16 text-center"><span className="text-xs text-slate-500">{monetary ? "Total · INR" : "Total assessments"}</span><strong className="mt-1 max-w-full break-words text-sm">{format(total)}</strong></div>
      </div>
      <div className="overflow-x-auto"><table className="w-full text-left text-xs"><caption className="sr-only">{title} exact values</caption><thead><tr className="border-b"><th className="py-2">Category</th><th className="text-right">{monetary ? "INR rupees" : "Count"}</th><th className="text-right">Share</th></tr></thead>
        <tbody>{data.map(slice => <tr key={slice.name} className="border-b border-slate-100"><td className="py-2 pr-2"><span aria-hidden="true" className="mr-2 inline-block h-2 w-2 rounded-full" style={{ backgroundColor: slice.color }} />{slice.name}</td><td className="whitespace-nowrap text-right">{format(slice.amount)}</td><td className="pl-2 text-right">{shareLabel(slice.amount, total)}</td></tr>)}</tbody></table></div>
    </>}
  </section>;
}
