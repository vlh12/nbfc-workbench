import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { moneyCents, moneyLabel } from "../dashboardMetrics";
import { portfolioDetails } from "../portfolioLabels";

type Nbfc = { nbfcId: string; loans: number; eadAmount: string | null };
export function PortfolioExposureChart({ selected, analysis, portfolio }: {
  selected: boolean;
  analysis: { snapshotId: string; nbfcs: Nbfc[] } | null;
  portfolio: { id: string; name?: string | null; reportingDate: string } | null;
}) {
  const ready = analysis && portfolio && analysis.snapshotId === portfolio.id;
  const rows = ready ? analysis.nbfcs : [];
  const complete = rows.length > 0 && rows.every(row => row.eadAmount != null);
  const money = (value: string) => moneyLabel(moneyCents(value));
  return <section aria-label="Exposure by NBFC" className="rounded-xl border border-slate-200 bg-white p-5">
    <h3 className="text-lg font-bold">Exposure by NBFC</h3>
    <p className="mt-2 text-sm text-slate-500">Explicit EAD totals in the selected portfolio, in INR. No stress assumptions or risk thresholds have been applied.</p>
    {ready && <p className="mt-2 break-words text-xs text-slate-500">{portfolioDetails(portfolio)} · {portfolio.reportingDate}</p>}
    {!ready ? <p className="py-6 text-slate-500">{selected ? "Waiting for selected portfolio analytics. Check any service error above if this persists." : "Select a saved portfolio to view exposure by NBFC."}</p>
      : !rows.length ? <p className="py-6 text-slate-500">This portfolio has no NBFC records.</p> : <>
        {complete ? <div className="mt-4 h-72" aria-label="Explicit EAD by NBFC chart; exact amounts are in the table below">
          <ResponsiveContainer width="100%" height="100%"><BarChart data={rows.map(row => ({ ...row, chartAmount: Number(row.eadAmount) }))}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="nbfcId" />
            <YAxis tickFormatter={value => (Number(value) / 10000000).toFixed(1) + " Cr"} />
            <Tooltip formatter={(_value, _name, item) => money((item.payload as Nbfc).eadAmount!)} />
            <Bar dataKey="chartAmount" name="Explicit EAD (INR)" fill="#0f766e" radius={[5, 5, 0, 0]} />
          </BarChart></ResponsiveContainer>
        </div> : <p className="my-4 text-sm text-amber-800">Incomplete EAD. Supply explicit EAD for every loan to display exposure totals. Missing amounts are not treated as zero.</p>}
        <div className="overflow-x-auto"><table className="mt-4 w-full text-sm">
          <caption className="sr-only">Selected portfolio explicit EAD totals by NBFC in INR</caption>
          <thead><tr className="border-b text-left"><th scope="col" className="py-3">NBFC</th><th scope="col" className="text-right">Explicit EAD (INR)</th><th scope="col" className="text-right">Loans</th></tr></thead>
          <tbody>{rows.map(row => <tr key={row.nbfcId} className="border-b border-slate-100"><td className="py-3">{row.nbfcId}</td><td className="text-right font-medium">{row.eadAmount == null ? "Not supplied" : money(row.eadAmount)}</td><td className="text-right">{row.loans}</td></tr>)}</tbody>
        </table></div>
      </>}
  </section>;
}
